package it.albemiglio.accounts.core.modules.replacers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * table-pattern discovers the target tables at execute time (SQL LIKE against the live schema) for
 * plugins that create one table per board/world/group — ajLeaderboards, BetterRTP, BetterEnderChest —
 * where the table names cannot be known when the template is written.
 */
class TablePatternReplacerTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    @Test
    void updatesEveryTableMatchingThePatternAndNoOther() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE board_a (uuid TEXT)");
                st.execute("CREATE TABLE board_b (uuid TEXT)");
                st.execute("CREATE TABLE players (uuid TEXT)");
                st.execute("INSERT INTO board_a VALUES ('" + OLD + "')");
                st.execute("INSERT INTO board_b VALUES ('" + OLD + "')");
                st.execute("INSERT INTO players VALUES ('" + OLD + "')");
            }

            pattern("board_%").replace(conn, OLD, NEW);

            assertEquals(NEW.toString(), single(conn, "SELECT uuid FROM board_a"));
            assertEquals(NEW.toString(), single(conn, "SELECT uuid FROM board_b"));
            assertEquals(OLD.toString(), single(conn, "SELECT uuid FROM players"));
        }
    }

    @Test
    void skipsAMatchedTableWithoutTheColumnAndLogsIt() throws Exception {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE board_a (uuid TEXT)");
                st.execute("CREATE TABLE board_meta (name TEXT)"); // matches the pattern, no uuid column
                st.execute("INSERT INTO board_a VALUES ('" + OLD + "')");
                st.execute("INSERT INTO board_meta VALUES ('skyblock')");
            }

            Logger logger = Logger.getLogger(ColumnReplacer.class.getName());
            logger.addHandler(handler);
            try {
                pattern("board_%").replace(conn, OLD, NEW);
            } finally {
                logger.removeHandler(handler);
            }

            assertEquals(NEW.toString(), single(conn, "SELECT uuid FROM board_a"));
            assertEquals("skyblock", single(conn, "SELECT name FROM board_meta"));
        }
        assertTrue(records.stream().anyMatch(r -> r.getMessage().contains("board_meta")),
                "expected a log line naming the skipped table");
    }

    @Test
    void tableAndTablePatternAreMutuallyExclusive() {
        assertThrows(IllegalArgumentException.class, () -> new ColumnReplacer(
                "board_a", "board_%", "uuid", UuidCodec.DASHED, null, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ColumnReplacer(
                null, null, "uuid", UuidCodec.DASHED, null, Collections.emptyList()));
    }

    private static ColumnReplacer pattern(String tablePattern) {
        return new ColumnReplacer(null, tablePattern, "uuid", UuidCodec.DASHED, null,
                Collections.emptyList());
    }

    private static String single(Connection conn, String sql) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
