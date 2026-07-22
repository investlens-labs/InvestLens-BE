package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Converts assessments created under the former five-point scale to the ten-point scale.
 *
 * <p>Only records last updated before V6 was installed are converted, so assessments produced
 * by the new 1-10 model are never re-scaled.</p>
 */
public class V7__rescale_existing_news_impact_scores extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        migrateScores(context.getConnection());
    }

    static void migrateScores(Connection connection) throws SQLException {
        Timestamp v6InstalledAt = v6InstalledAt(connection);
        if (v6InstalledAt == null) {
            throw new SQLException("Cannot rescale scores: V6 migration timestamp was not found");
        }

        updateImpacts(connection, v6InstalledAt);
        updateTranslations(connection, v6InstalledAt);
    }

    private static Timestamp v6InstalledAt(Connection connection) throws SQLException {
        String sql = "SELECT installed_on FROM flyway_schema_history "
                + "WHERE version = '6' AND success = TRUE "
                + "ORDER BY installed_rank DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getTimestamp("installed_on") : null;
        }
    }

    private static void updateImpacts(Connection connection, Timestamp cutoff) throws SQLException {
        String sql = "UPDATE news_impacts SET score = " + scaledValue("score")
                + " WHERE score BETWEEN 1 AND 5 AND news_id IN ("
                + "SELECT id FROM news_articles WHERE updated_at < ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, cutoff);
            statement.executeUpdate();
        }
    }

    private static void updateTranslations(Connection connection, Timestamp cutoff) throws SQLException {
        String sql = "UPDATE news_translations SET impact_score = " + scaledValue("impact_score")
                + " WHERE impact_score BETWEEN 1 AND 5 AND updated_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, cutoff);
            statement.executeUpdate();
        }
    }

    private static String scaledValue(String column) {
        return "CASE " + column
                + " WHEN 1 THEN 1 WHEN 2 THEN 3 WHEN 3 THEN 5 WHEN 4 THEN 8 WHEN 5 THEN 10"
                + " ELSE " + column + " END";
    }
}
