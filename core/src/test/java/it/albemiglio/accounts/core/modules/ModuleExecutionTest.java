package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.modules.replacers.ColumnReplacer;
import it.albemiglio.accounts.core.modules.replacers.Replacer;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleExecutionTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    static class TestModule extends Module {
        TestModule(DB db) {
            super("test", Platform.BUNGEECORD, db);
        }

        void register(Replacer replacer) {
            addReplacer(replacer);
        }
    }

    @Test
    void commitsEveryReplacerWhenAllSucceed(@TempDir Path dir) throws Exception {
        DB db = new SQLite(null, 0, null, null, dir.resolve("a.db").toString());
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE accounts (uuid TEXT)");
            st.execute("CREATE TABLE homes (uuid TEXT)");
            st.execute("INSERT INTO accounts VALUES ('" + OLD + "')");
            st.execute("INSERT INTO homes VALUES ('" + OLD + "')");
        }

        TestModule module = new TestModule(db);
        module.register(new ColumnReplacer("accounts", "uuid"));
        module.register(new ColumnReplacer("homes", "uuid"));

        module.execute(Pair.of(OLD, NEW));

        assertEquals(NEW.toString(), single(db, "SELECT uuid FROM accounts"));
        assertEquals(NEW.toString(), single(db, "SELECT uuid FROM homes"));
        db.close();
    }

    @Test
    void rollsBackEveryChangeWhenAnyReplacerFails(@TempDir Path dir) throws Exception {
        DB db = new SQLite(null, 0, null, null, dir.resolve("b.db").toString());
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE accounts (uuid TEXT)");
            st.execute("INSERT INTO accounts VALUES ('" + OLD + "')");
            // no "ghost" table on purpose: the second replacer must fail
        }

        TestModule module = new TestModule(db);
        module.register(new ColumnReplacer("accounts", "uuid")); // runs first, would succeed
        module.register(new ColumnReplacer("ghost", "uuid"));    // fails: no such table

        assertThrows(MigrationException.class, () -> module.execute(Pair.of(OLD, NEW)));

        assertEquals(OLD.toString(), single(db, "SELECT uuid FROM accounts"));
        db.close();
    }

    private static String single(DB db, String sql) throws Exception {
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
