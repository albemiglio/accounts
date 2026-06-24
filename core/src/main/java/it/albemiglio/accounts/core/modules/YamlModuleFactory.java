package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.database.MariaDB;
import it.albemiglio.accounts.core.database.MySQL;
import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.modules.replacers.ColumnReplacer;
import it.albemiglio.accounts.core.modules.replacers.Replacer;
import it.albemiglio.accounts.core.objects.enums.DBType;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlModuleFactory {

    public Module build(Map<String, Object> config) {
        String name = (String) config.get("name");
        Platform platform = Platform.valueOf(((String) config.get("platform")).toUpperCase());
        String type = (String) config.getOrDefault("type", "sql");

        Module module = "file".equalsIgnoreCase(type)
                ? buildFileModule(name, platform, config)
                : buildSqlModule(name, platform, config);

        if (Boolean.TRUE.equals(config.get("enabled"))) {
            module.enable();
        }
        return module;
    }

    private Module buildFileModule(String name, Platform platform, Map<String, Object> config) {
        Path directory = Path.of((String) config.get("directory"));
        String extension = (String) config.get("extension");
        return new FileModule(name, platform, directory, extension);
    }

    @SuppressWarnings("unchecked")
    private Module buildSqlModule(String name, Platform platform, Map<String, Object> config) {
        DB database = buildDatabase((Map<String, Object>) config.get("database"));
        List<Replacer> replacers = new ArrayList<>();
        List<Map<String, Object>> replacerConfigs = (List<Map<String, Object>>) config.get("replacers");
        if (replacerConfigs != null) {
            for (Map<String, Object> replacer : replacerConfigs) {
                replacers.add(new ColumnReplacer((String) replacer.get("table"), (String) replacer.get("column")));
            }
        }
        return new YamlModule(name, platform, database, replacers);
    }

    private DB buildDatabase(Map<String, Object> config) {
        DBType type = DBType.valueOf(((String) config.get("type")).toUpperCase());
        String host = (String) config.get("host");
        int port = config.get("port") == null ? 0 : ((Number) config.get("port")).intValue();
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        String database = (String) config.get("database");

        switch (type) {
            case SQLITE:
                return new SQLite(host, port, username, password, database);
            case MYSQL:
                return new MySQL(host, port, username, password, database);
            case MARIADB:
                return new MariaDB(host, port, username, password, database);
            case H2:
                throw new IllegalArgumentException(
                        "H2 cannot be migrated live: the plugin owning the .mv.db file holds an exclusive "
                        + "lock. Configure that plugin to use SQLite or MySQL instead (both migrate fine).");
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
}
