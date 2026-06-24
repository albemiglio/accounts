package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.objects.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlModuleFactoryTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    @Test
    void buildsModuleThatMigratesEveryConfiguredColumn(@TempDir Path dir) throws Exception {
        String dbFile = dir.resolve("data.db").toString();
        DB seed = new SQLite(null, 0, null, null, dbFile);
        try (Connection c = seed.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE accounts (uuid TEXT)");
            st.execute("CREATE TABLE homes (owner TEXT)");
            st.execute("INSERT INTO accounts VALUES ('" + OLD + "')");
            st.execute("INSERT INTO homes VALUES ('" + OLD + "')");
        }
        seed.close();

        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("type", "SQLITE");
        dbConfig.put("database", dbFile);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", "test");
        config.put("platform", "BUNGEECORD");
        config.put("database", dbConfig);
        config.put("replacers", List.of(
                Map.of("table", "accounts", "column", "uuid"),
                Map.of("table", "homes", "column", "owner")));

        Module module = new YamlModuleFactory().build(config);
        module.enable();
        module.execute(Pair.of(OLD, NEW));

        DB check = new SQLite(null, 0, null, null, dbFile);
        assertEquals(NEW.toString(), single(check, "SELECT uuid FROM accounts"));
        assertEquals(NEW.toString(), single(check, "SELECT owner FROM homes"));
        check.close();
    }

    @Test
    void enablesModuleWhenConfigRequestsIt() {
        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("type", "SQLITE");
        dbConfig.put("database", ":memory:");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", "x");
        config.put("platform", "BUNGEECORD");
        config.put("enabled", true);
        config.put("database", dbConfig);

        Module module = new YamlModuleFactory().build(config);

        assertTrue(module.isEnabled());
    }

    @Test
    void rejectsUnsupportedDatabaseType() {
        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("type", "MONGODB");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", "x");
        config.put("platform", "BUNGEECORD");
        config.put("database", dbConfig);

        assertThrows(IllegalArgumentException.class, () -> new YamlModuleFactory().build(config));
    }

    @Test
    void buildsAFileModuleWhenTypeIsFile(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve(OLD + ".yml"), "money: 5");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", "essentials");
        config.put("platform", "SPIGOT");
        config.put("type", "file");
        config.put("directory", dir.toString());
        config.put("extension", "yml");
        config.put("enabled", true);

        Module module = new YamlModuleFactory().build(config);
        module.execute(Pair.of(OLD, NEW));

        assertTrue(module.isEnabled());
        assertTrue(Files.exists(dir.resolve(NEW + ".yml")));
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
