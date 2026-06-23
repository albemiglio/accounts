package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.database.MariaDB;
import it.albemiglio.accounts.core.database.MySQL;
import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.modules.replacers.ColumnReplacer;
import it.albemiglio.accounts.core.modules.replacers.Replacer;
import it.albemiglio.accounts.core.objects.enums.DBType;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlModuleFactory {

    @SuppressWarnings("unchecked")
    public Module build(Map<String, Object> config) {
        String name = (String) config.get("name");
        Platform platform = Platform.valueOf(((String) config.get("platform")).toUpperCase());
        DB database = buildDatabase((Map<String, Object>) config.get("database"));

        List<Replacer> replacers = new ArrayList<>();
        List<Map<String, Object>> replacerConfigs = (List<Map<String, Object>>) config.get("replacers");
        if (replacerConfigs != null) {
            for (Map<String, Object> replacer : replacerConfigs) {
                replacers.add(new ColumnReplacer((String) replacer.get("table"), (String) replacer.get("column")));
            }
        }

        YamlModule module = new YamlModule(name, platform, database, replacers);
        if (Boolean.TRUE.equals(config.get("enabled"))) {
            module.enable();
        }
        return module;
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
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
}
