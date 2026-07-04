package it.albemiglio.accounts.core.modules.replacers;

import it.albemiglio.accounts.core.modules.Diagnosis;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The read-only pre-flight behind {@code /accounts diagnose}: it must confirm a real uuid is present in
 * the configured encoding (VERIFIED), and — the point of the feature — catch a template whose {@code
 * format} doesn't match how the data is actually stored (FORMAT_MISMATCH), which a blind migration would
 * silently miss.
 */
class DiagnoseTest {

    private static final UUID PROBE = new UUID(0x069a79f444e94726L, 0xa5befca90e38aaf5L);
    private static final UUID OTHER = new UUID(0L, 99L);

    private Connection dashedTable() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE players (uuid TEXT, name TEXT)");
            st.execute("INSERT INTO players VALUES ('" + PROBE + "', 'Notch')");
        }
        return conn;
    }

    @Test
    void verifiesWhenTheConfiguredFormatMatches() throws Exception {
        try (Connection conn = dashedTable()) {
            List<Diagnosis> out = new ColumnReplacer("players", "uuid", UuidCodec.DASHED)
                    .diagnose(conn, PROBE, "mod");
            assertEquals(1, out.size());
            assertEquals(Diagnosis.Status.VERIFIED, out.get(0).getStatus());
            assertEquals(1, out.get(0).getOccurrences());
        }
    }

    @Test
    void flagsAFormatMismatch() throws Exception {
        // Stored dashed, but the template configures undashed -> a blind migration would miss the row.
        try (Connection conn = dashedTable()) {
            List<Diagnosis> out = new ColumnReplacer("players", "uuid", UuidCodec.UNDASHED)
                    .diagnose(conn, PROBE, "mod");
            assertEquals(Diagnosis.Status.FORMAT_MISMATCH, out.get(0).getStatus());
            assertTrue(out.get(0).isBlocker());
            assertTrue(out.get(0).getDetail().contains("dashed"));
        }
    }

    @Test
    void notFoundWhenThePlayerHasNoRow() throws Exception {
        try (Connection conn = dashedTable()) {
            List<Diagnosis> out = new ColumnReplacer("players", "uuid", UuidCodec.DASHED)
                    .diagnose(conn, OTHER, "mod");
            assertEquals(Diagnosis.Status.NOT_FOUND, out.get(0).getStatus());
        }
    }

    @Test
    void missingWhenTheTableIsAbsent() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            List<Diagnosis> out = new ColumnReplacer("ghosts", "uuid", UuidCodec.DASHED)
                    .diagnose(conn, PROBE, "mod");
            assertEquals(Diagnosis.Status.MISSING, out.get(0).getStatus());
        }
    }
}
