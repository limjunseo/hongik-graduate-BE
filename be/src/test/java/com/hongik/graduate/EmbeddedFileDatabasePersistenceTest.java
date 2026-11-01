package com.hongik.graduate;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Confirms the embedded file DB used by the local app survives closing and
 * reopening its JDBC connection without any separately managed DB server.
 */
class EmbeddedFileDatabasePersistenceTest {
    @TempDir Path tempDir;

    @Test
    void valuesSurviveClosingAndReopeningDatabase() throws Exception {
        Path base = tempDir.resolve("career-workspace");
        String url = "jdbc:h2:file:" + base.toAbsolutePath().toString().replace('\\', '/');
        String marker = UUID.randomUUID().toString();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE local_persistence_test (id INTEGER PRIMARY KEY, marker VARCHAR(50))");
            try (var insert = connection.prepareStatement(
                    "INSERT INTO local_persistence_test (id, marker) VALUES (1, ?)")) {
                insert.setString(1, marker);
                insert.executeUpdate();
            }
        }

        assertThat(Files.exists(Path.of(base + ".mv.db"))).isTrue();

        try (var reopened = DriverManager.getConnection(url, "sa", "");
             var statement = reopened.createStatement();
             var result = statement.executeQuery(
                     "SELECT marker FROM local_persistence_test WHERE id = 1")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo(marker);
            assertThat(result.next()).isFalse();
        }
    }
}
