package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__expand_news_impact_score_range extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        replaceScoreConstraint(context.getConnection(), "news_impacts", "score", "chk_news_impacts_score_range");
        replaceScoreConstraint(context.getConnection(), "news_translations", "impact_score",
                "chk_news_translations_impact_score_range");
    }

    private static void replaceScoreConstraint(Connection connection, String table, String column,
                                               String replacementConstraint) throws SQLException {
        for (String constraint : scoreConstraints(connection, table, column)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + quoteIdentifier(constraint));
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + replacementConstraint
                    + " CHECK (" + column + " BETWEEN 1 AND 10)");
        }
    }

    private static List<String> scoreConstraints(Connection connection, String table, String column)
            throws SQLException {
        String sql = "SELECT tc.constraint_name, cc.check_clause "
                + "FROM information_schema.table_constraints tc "
                + "JOIN information_schema.check_constraints cc "
                + "USING (constraint_catalog, constraint_schema, constraint_name) "
                + "WHERE LOWER(tc.table_name) = LOWER(?) AND tc.constraint_type = 'CHECK'";
        List<String> constraints = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String clause = result.getString("check_clause");
                    if (clause != null && clause.toLowerCase(java.util.Locale.ROOT).contains(column)) {
                        constraints.add(result.getString("constraint_name"));
                    }
                }
            }
        }
        return constraints;
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
