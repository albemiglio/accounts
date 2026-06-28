package it.albemiglio.accounts.core.modules.replacers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the non-dashed encodings a UUID column can use. SQLite has no BINARY(16), so the byte path runs
 * against a BLOB column holding the same 16 bytes setBytes would bind to a real BINARY(16) on MySQL.
 */
class UuidCodecColumnReplacerTest {

    private static final UUID OLD = new UUID(0x069a79f444e94726L, 0xa5befca90e38aaf5L);
    private static final UUID NEW = new UUID(0L, 2L);
    private static final UUID OTHER = new UUID(0L, 99L);

    @Test
    void rewritesABinaryColumn() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE players (uuid BLOB, name TEXT)");
            }
            insertBytes(conn, OLD, "Notch");
            insertBytes(conn, OTHER, "Jeb");

            new ColumnReplacer("players", "uuid", UuidCodec.BINARY).replace(conn, OLD, NEW);

            assertArrayEquals(UuidCodec.toBytes(NEW), bytesOf(conn, "Notch"));
            assertArrayEquals(UuidCodec.toBytes(OTHER), bytesOf(conn, "Jeb"));
        }
    }

    @Test
    void rewritesAnUndashedColumn() throws Exception {
        String oldHex = OLD.toString().replace("-", "");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE players (uuid TEXT, name TEXT)");
                st.execute("INSERT INTO players VALUES ('" + oldHex + "', 'Notch')");
                st.execute("INSERT INTO players VALUES ('" + OTHER.toString().replace("-", "") + "', 'Jeb')");
            }

            new ColumnReplacer("players", "uuid", UuidCodec.UNDASHED).replace(conn, OLD, NEW);

            String migrated = textOf(conn, "Notch");
            assertEquals(NEW.toString().replace("-", ""), migrated);
            assertFalse(migrated.contains("-"));
            assertEquals(OTHER.toString().replace("-", ""), textOf(conn, "Jeb"));
        }
    }

    @Test
    void binaryBindsSixteenBytesMostSignificantFirst() {
        byte[] bytes = UuidCodec.toBytes(OLD);
        assertEquals(16, bytes.length);
        assertEquals((byte) 0x06, bytes[0]);
        assertEquals((byte) 0xf5, bytes[15]);
    }

    @Test
    void leavesABinaryColumnUntouchedWhenNothingMatches() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE players (uuid BLOB, name TEXT)");
            }
            insertBytes(conn, OTHER, "Jeb");

            new ColumnReplacer("players", "uuid", UuidCodec.BINARY).replace(conn, OLD, NEW);

            assertArrayEquals(UuidCodec.toBytes(OTHER), bytesOf(conn, "Jeb"));
        }
    }

    @Test
    void defaultsToDashedWhenFormatIsAbsent() {
        assertTrue(UuidCodec.of(null) == UuidCodec.DASHED);
        assertTrue(UuidCodec.of("binary") == UuidCodec.BINARY);
    }

    private static void insertBytes(Connection conn, UUID id, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO players VALUES (?, ?)")) {
            ps.setBytes(1, UuidCodec.toBytes(id));
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    private static byte[] bytesOf(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM players WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBytes("uuid");
            }
        }
    }

    private static String textOf(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM players WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString("uuid");
            }
        }
    }
}
