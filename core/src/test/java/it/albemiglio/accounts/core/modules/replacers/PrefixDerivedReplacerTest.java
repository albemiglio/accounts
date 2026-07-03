package it.albemiglio.accounts.core.modules.replacers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuxProtect-style columns: the stored value is {@code '$' + dashed uuid} and the plugin looks rows up
 * by a companion hash column holding {@code String.hashCode()} of that full string. Rewriting the uuid
 * without the prefix matches nothing; rewriting it without the hash bricks the plugin — so matching
 * uses prefix + encoding and every derived column is written by the same UPDATE.
 */
class PrefixDerivedReplacerTest {

    private static final UUID OLD = new UUID(0x069a79f444e94726L, 0xa5befca90e38aaf5L);
    private static final UUID NEW = new UUID(0L, 2L);
    private static final UUID OTHER = new UUID(0L, 99L);

    @Test
    void prefixMatchesAndRewritesOnlyThePrefixedForm() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE auxprotect_uids (uuid TEXT, name TEXT)");
                st.execute("INSERT INTO auxprotect_uids VALUES ('$" + OLD + "', 'Notch')");
                // The same uuid without the prefix must stay untouched: it is a different stored value.
                st.execute("INSERT INTO auxprotect_uids VALUES ('" + OLD + "', 'Impostor')");
            }

            replacer("$", Collections.emptyList()).replace(conn, OLD, NEW);

            assertEquals("$" + NEW, textOf(conn, "Notch"));
            assertEquals(OLD.toString(), textOf(conn, "Impostor"));
        }
    }

    @Test
    void prefixWorksWithUndashedFormatToo() throws Exception {
        String oldStored = "$" + OLD.toString().replace("-", "");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE auxprotect_uids (uuid TEXT, name TEXT)");
                st.execute("INSERT INTO auxprotect_uids VALUES ('" + oldStored + "', 'Notch')");
            }

            new ColumnReplacer("auxprotect_uids", null, "uuid", UuidCodec.UNDASHED, "$",
                    Collections.emptyList()).replace(conn, OLD, NEW);

            assertEquals("$" + NEW.toString().replace("-", ""), textOf(conn, "Notch"));
        }
    }

    @Test
    void derivedHashIsRecomputedFromTheNewStoredStringInTheSameUpdate() throws Exception {
        String oldStored = "$" + OLD;
        String otherStored = "$" + OTHER;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE auxprotect_uids (uuid TEXT, hash INTEGER, name TEXT)");
                st.execute("INSERT INTO auxprotect_uids VALUES ('" + oldStored + "', "
                        + oldStored.hashCode() + ", 'Notch')");
                st.execute("INSERT INTO auxprotect_uids VALUES ('" + otherStored + "', "
                        + otherStored.hashCode() + ", 'Jeb')");
            }

            replacer("$", List.of(new ColumnReplacer.Derived("hash", "java-string-hashcode")))
                    .replace(conn, OLD, NEW);

            String newStored = "$" + NEW;
            assertEquals(newStored, textOf(conn, "Notch"));
            assertEquals(newStored.hashCode(), hashOf(conn, "Notch"));
            assertEquals(otherStored.hashCode(), hashOf(conn, "Jeb"));
        }
    }

    @Test
    void unknownDerivedFnIsRejectedAtBuildTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new ColumnReplacer.Derived("hash", "md5"));
    }

    @Test
    void prefixCannotBeCombinedWithBinaryFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> new ColumnReplacer("t", null, "uuid", UuidCodec.BINARY, "$",
                        Collections.emptyList()));
    }

    @Test
    void derivedCannotBeCombinedWithBinaryFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> new ColumnReplacer("t", null, "uuid", UuidCodec.BINARY, null,
                        List.of(new ColumnReplacer.Derived("hash", "java-string-hashcode"))));
    }

    private static ColumnReplacer replacer(String prefix, List<ColumnReplacer.Derived> derived) {
        return new ColumnReplacer("auxprotect_uids", null, "uuid", UuidCodec.DASHED, prefix, derived);
    }

    private static String textOf(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM auxprotect_uids WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString("uuid");
            }
        }
    }

    private static int hashOf(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT hash FROM auxprotect_uids WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("hash");
            }
        }
    }
}
