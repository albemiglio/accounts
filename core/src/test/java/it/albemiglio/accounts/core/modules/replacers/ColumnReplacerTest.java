package it.albemiglio.accounts.core.modules.replacers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnReplacerTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    @Test
    void rewritesMatchingUuidInColumn() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE players (uuid TEXT, name TEXT)");
                st.execute("INSERT INTO players VALUES ('" + OLD + "', 'Notch')");
            }

            new ColumnReplacer("players", "uuid").replace(conn, OLD, NEW);

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT uuid FROM players WHERE name = 'Notch'")) {
                rs.next();
                assertEquals(NEW.toString(), rs.getString("uuid"));
            }
        }
    }

    @Test
    void leavesNonMatchingRowsUntouched() throws Exception {
        UUID other = new UUID(0L, 99L);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE players (uuid TEXT, name TEXT)");
                st.execute("INSERT INTO players VALUES ('" + OLD + "', 'Notch')");
                st.execute("INSERT INTO players VALUES ('" + other + "', 'Jeb')");
            }

            new ColumnReplacer("players", "uuid").replace(conn, OLD, NEW);

            assertEquals(NEW.toString(), uuidOf(conn, "Notch"));
            assertEquals(other.toString(), uuidOf(conn, "Jeb"));
        }
    }

    @Test
    void rewritesEveryRowReferencingOldId() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE homes (uuid TEXT, label TEXT)");
                st.execute("INSERT INTO homes VALUES ('" + OLD + "', 'home')");
                st.execute("INSERT INTO homes VALUES ('" + OLD + "', 'shop')");
            }

            new ColumnReplacer("homes", "uuid").replace(conn, OLD, NEW);

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM homes WHERE uuid = '" + NEW + "'")) {
                rs.next();
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    private static String uuidOf(Connection conn, String name) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid FROM players WHERE name = '" + name + "'")) {
            rs.next();
            return rs.getString("uuid");
        }
    }
}
