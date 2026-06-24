package it.albemiglio.accounts.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import it.albemiglio.accounts.core.services.BatchService;
import it.albemiglio.accounts.core.services.ModuleService;
import it.albemiglio.accounts.core.services.RedisService;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Velocity entry point: boots the migration engine and exposes {@code /accounts migrate}. The proxy
 * is where a network-wide UUID migration is driven from, with the per-database work done by the
 * modules configured in the data folder.
 */
@Plugin(id = "accounts", name = "Accounts", version = "1.0-SNAPSHOT", authors = {"albemiglio"})
public class AccountsPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private RedisService redis;

    @Inject
    public AccountsPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        try {
            Map<String, Object> config = loadConfig();
            Map<String, Object> redisConfig = section(config, "redis");
            this.redis = new RedisService(
                    (String) redisConfig.getOrDefault("host", "localhost"),
                    ((Number) redisConfig.getOrDefault("port", 6379)).intValue(),
                    (String) redisConfig.getOrDefault("password", ""));
            redis.start();

            ModuleService moduleService = new ModuleService(redis);
            Path modulesDir = dataDirectory.resolve((String) config.getOrDefault("modules-dir", "modules"));
            Files.createDirectories(modulesDir);
            moduleService.loadModules(modulesDir);
            redis.updateActiveModules(moduleService.getModules().size());

            BatchService batchService = new BatchService(redis, moduleService.getModules());

            CommandManager commands = proxy.getCommandManager();
            CommandMeta meta = commands.metaBuilder("accounts").build();
            commands.register(meta, new MigrateCommand(batchService));

            logger.info("Accounts ready: {} module(s) loaded", moduleService.getModules().size());
        } catch (Exception e) {
            logger.error("Accounts failed to start", e);
        }
    }

    private Map<String, Object> loadConfig() throws IOException {
        Files.createDirectories(dataDirectory);
        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream defaults = getClass().getResourceAsStream("/config.yml")) {
                if (defaults != null) {
                    Files.copy(defaults, configFile);
                }
            }
        }
        try (InputStream in = Files.newInputStream(configFile)) {
            Map<String, Object> loaded = new Yaml().load(in);
            return loaded != null ? loaded : Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }
}
