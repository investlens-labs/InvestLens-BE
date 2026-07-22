package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class V7__rescale_existing_news_impact_scoresTest {
    @Test
    void rescalesOnlyAssessmentsCreatedBeforeTheTenPointScale() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:score-rescale;MODE=PostgreSQL")) {
            createSchema(connection);
            Timestamp v6InstalledAt = Timestamp.from(Instant.parse("2026-07-22T12:07:27Z"));
            insertMigration(connection, v6InstalledAt);
            insertArticle(connection, "legacy", v6InstalledAt, -1);
            insertArticle(connection, "current", v6InstalledAt, 1);
            insertImpact(connection, "legacy", 1);
            insertImpact(connection, "legacy", 2);
            insertImpact(connection, "legacy", 3);
            insertImpact(connection, "legacy", 4);
            insertImpact(connection, "legacy", 5);
            insertImpact(connection, "current", 5);
            insertTranslation(connection, "legacy", 4, v6InstalledAt, -1);
            insertTranslation(connection, "current", 5, v6InstalledAt, 1);

            V7__rescale_existing_news_impact_scores.migrateScores(connection);

            assertThat(scores(connection, "legacy")).containsExactly(1, 3, 5, 8, 10);
            assertThat(scores(connection, "current")).containsExactly(5);
            assertThat(translationScore(connection, "legacy")).isEqualTo(8);
            assertThat(translationScore(connection, "current")).isEqualTo(5);
        }
    }

    private static void createSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE flyway_schema_history (installed_rank INT, version VARCHAR(20), installed_on TIMESTAMP, success BOOLEAN)");
            statement.execute("CREATE TABLE news_articles (id VARCHAR(20) PRIMARY KEY, updated_at TIMESTAMP)");
            statement.execute("CREATE TABLE news_impacts (news_id VARCHAR(20), score INT)");
            statement.execute("CREATE TABLE news_translations (news_id VARCHAR(20), impact_score INT, updated_at TIMESTAMP)");
        }
    }

    private static void insertMigration(Connection connection, Timestamp installedAt) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO flyway_schema_history VALUES (6, '6', ?, TRUE)")) {
            statement.setTimestamp(1, installedAt);
            statement.executeUpdate();
        }
    }

    private static void insertArticle(Connection connection, String id, Timestamp cutoff, int secondsOffset) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO news_articles VALUES (?, ?)")) {
            statement.setString(1, id);
            statement.setTimestamp(2, Timestamp.from(cutoff.toInstant().plusSeconds(secondsOffset)));
            statement.executeUpdate();
        }
    }

    private static void insertImpact(Connection connection, String newsId, int score) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO news_impacts VALUES (?, ?)")) {
            statement.setString(1, newsId);
            statement.setInt(2, score);
            statement.executeUpdate();
        }
    }

    private static void insertTranslation(Connection connection, String newsId, int score, Timestamp cutoff,
                                          int secondsOffset) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO news_translations VALUES (?, ?, ?)")) {
            statement.setString(1, newsId);
            statement.setInt(2, score);
            statement.setTimestamp(3, Timestamp.from(cutoff.toInstant().plusSeconds(secondsOffset)));
            statement.executeUpdate();
        }
    }

    private static java.util.List<Integer> scores(Connection connection, String newsId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT score FROM news_impacts WHERE news_id = ? ORDER BY score")) {
            statement.setString(1, newsId);
            try (ResultSet result = statement.executeQuery()) {
                java.util.List<Integer> scores = new java.util.ArrayList<>();
                while (result.next()) scores.add(result.getInt(1));
                return scores;
            }
        }
    }

    private static int translationScore(Connection connection, String newsId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT impact_score FROM news_translations WHERE news_id = ?")) {
            statement.setString(1, newsId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
