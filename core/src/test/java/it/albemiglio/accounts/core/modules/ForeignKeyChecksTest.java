package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.modules.replacers.ColumnReplacer;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.DBType;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * disable-foreign-key-checks: plugins like HuskSync and BattlePass declare foreign keys between their
 * uuid columns without ON UPDATE CASCADE, so whichever table a replacer updates first violates the
 * constraint. The flag suspends enforcement around the module's transaction (MySQL/MariaDB
 * FOREIGN_KEY_CHECKS, H2 REFERENTIAL_INTEGRITY) and is a logged no-op where there is nothing to
 * suspend (SQLite). The H2 pair below is the real proof: the same migration fails without the flag.
 */
class ForeignKeyChecksTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    @Test
    void sqliteIgnoresTheFlagWithALogLineAndStillMigrates(@TempDir Path dir) throws Exception {
        String dbFile = dir.resolve("data.db").toString();
        DB seed = new SQLite(null, 0, null, null, dbFile);
        try (Connection c = seed.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE accounts (uuid TEXT)");
            st.execute("INSERT INTO accounts VALUES ('" + OLD + "')");
        }
        seed.close();

        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("type", "SQLITE");
        dbConfig.put("database", dbFile);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", "husksync");
        config.put("platform", "SPIGOT");
        config.put("disable-foreign-key-checks", true);
        config.put("database", dbConfig);
        config.put("replacers", List.of(Map.of("table", "accounts", "column", "uuid")));

        Module module = new YamlModuleFactory().build(config);

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
        Logger logger = Logger.getLogger(Module.class.getName());
        logger.addHandler(handler);
        try {
            module.execute(Pair.of(OLD, NEW));
        } finally {
            logger.removeHandler(handler);
        }

        DB check = new SQLite(null, 0, null, null, dbFile);
        assertEquals(NEW.toString(), single(check, "SELECT uuid FROM accounts"));
        check.close();
        assertTrue(records.stream().anyMatch(r -> r.getMessage().contains("disable-foreign-key-checks")),
                "expected a log line saying the flag was ignored");
    }

    @Test
    void mysqlAndMariaDbWrapTheReplacersInForeignKeyChecksToggles() {
        for (DBType type : new DBType[]{DBType.MYSQL, DBType.MARIADB}) {
            RecordingDb db = new RecordingDb(type);
            Module module = new YamlModule("husksync", Platform.SPIGOT, db,
                    List.of(new ColumnReplacer("husksync_users", "uuid")), true);

            module.execute(Pair.of(OLD, NEW));

            assertEquals(3, db.statements.size(), db.statements.toString());
            assertEquals("SET FOREIGN_KEY_CHECKS=0", db.statements.get(0));
            assertTrue(db.statements.get(1).startsWith("UPDATE husksync_users"), db.statements.get(1));
            assertEquals("SET FOREIGN_KEY_CHECKS=1", db.statements.get(2));
        }
    }

    @Test
    void withoutTheFlagNoToggleIsEmitted() {
        RecordingDb db = new RecordingDb(DBType.MYSQL);
        Module module = new YamlModule("husksync", Platform.SPIGOT, db,
                List.of(new ColumnReplacer("husksync_users", "uuid")));

        module.execute(Pair.of(OLD, NEW));

        assertEquals(1, db.statements.size(), db.statements.toString());
        assertTrue(db.statements.get(0).startsWith("UPDATE"), db.statements.get(0));
    }

    @Test
    void h2SuspendsReferentialIntegrityAcrossTheTransaction() throws Exception {
        MemH2 db = new MemH2("fk_flag_on");
        seedParentChild(db);

        Module module = new YamlModule("battlepass", Platform.SPIGOT, db, List.of(
                new ColumnReplacer("parent", "uuid"),
                new ColumnReplacer("child", "uuid")), true);
        module.execute(Pair.of(OLD, NEW));

        assertEquals(NEW.toString(), single(db, "SELECT uuid FROM parent"));
        assertEquals(NEW.toString(), single(db, "SELECT uuid FROM child"));
    }

    @Test
    void withoutTheFlagTheForeignKeyFailsTheMigrationAndRollsBack() throws Exception {
        MemH2 db = new MemH2("fk_flag_off");
        seedParentChild(db);

        Module module = new YamlModule("battlepass", Platform.SPIGOT, db, List.of(
                new ColumnReplacer("parent", "uuid"),
                new ColumnReplacer("child", "uuid")));
        assertThrows(MigrationException.class, () -> module.execute(Pair.of(OLD, NEW)));

        assertEquals(OLD.toString(), single(db, "SELECT uuid FROM parent"));
        assertEquals(OLD.toString(), single(db, "SELECT uuid FROM child"));
    }

    private static void seedParentChild(DB db) throws Exception {
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE parent (uuid VARCHAR(36) PRIMARY KEY)");
            st.execute("CREATE TABLE child (uuid VARCHAR(36), FOREIGN KEY (uuid) REFERENCES parent(uuid))");
            st.execute("INSERT INTO parent VALUES ('" + OLD + "')");
            st.execute("INSERT INTO child VALUES ('" + OLD + "')");
        }
    }

    private static String single(DB db, String sql) throws Exception {
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    /** Records every statement text a module runs, so MySQL emission is testable without a server. */
    static class RecordingDb extends DB {

        final List<String> statements = new ArrayList<>();

        RecordingDb(DBType type) {
            super(null, 0, null, null, "stub");
            this.type = type;
        }

        @Override
        public String jdbcUrl() {
            return "jdbc:stub";
        }

        @Override
        public String driverClassName() {
            return "stub";
        }

        @Override
        public synchronized Connection getConnection() {
            return (Connection) proxy(Connection.class);
        }

        private Object proxy(Class<?> face) {
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{face},
                    (instance, method, args) -> {
                        String name = method.getName();
                        if ((name.equals("execute") || name.equals("prepareStatement"))
                                && args != null && args.length > 0 && args[0] instanceof String) {
                            statements.add((String) args[0]);
                        }
                        Class<?> returned = method.getReturnType();
                        if (returned == Statement.class || returned == PreparedStatement.class) {
                            return proxy(returned);
                        }
                        if (returned == boolean.class) {
                            return false;
                        }
                        if (returned == int.class) {
                            return 0;
                        }
                        return null;
                    });
        }
    }

    /** A real in-memory H2 (test-scoped driver) that enforces foreign keys, unlike SQLite. */
    static class MemH2 extends DB {

        private final String url;

        MemH2(String name) {
            super(null, 0, "sa", "", name);
            this.type = DBType.H2;
            this.url = "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
        }

        @Override
        public String jdbcUrl() {
            return url;
        }

        @Override
        public String driverClassName() {
            return "org.h2.Driver";
        }

        @Override
        public synchronized Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, "sa", "");
        }
    }
}
