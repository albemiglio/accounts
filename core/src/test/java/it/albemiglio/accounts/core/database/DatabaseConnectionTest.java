package it.albemiglio.accounts.core.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConnectionTest {

    @Test
    void providesUsableConnectionsBackedByOneDatabase(@TempDir Path dir) throws Exception {
        DB db = new SQLite(null, 0, null, null, dir.resolve("accounts.db").toString());
        try {
            try (Connection conn = db.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE t (x INTEGER)");
                st.execute("INSERT INTO t VALUES (1)");
            }

            // A second borrow must see the committed data (same underlying database).
            try (Connection conn = db.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM t")) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }
        } finally {
            db.close();
        }
    }
}
