package it.albemiglio.accounts.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import it.albemiglio.accounts.core.services.AccountsEngine;
import it.albemiglio.accounts.core.services.InstanceId;
import it.albemiglio.accounts.core.services.ModuleService;
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

    private AccountsEngine engine;

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

            ModuleService moduleService = new ModuleService(count -> { });
            Path modulesDir = dataDirectory.resolve((String) config.getOrDefault("modules-dir", "modules"));
            Files.createDirectories(modulesDir);
            moduleService.loadModules(modulesDir);

            this.engine = AccountsEngine.start(
                    (String) redisConfig.getOrDefault("host", "localhost"),
                    ((Number) redisConfig.getOrDefault("port", 6379)).intValue(),
                    (String) redisConfig.getOrDefault("password", ""),
                    InstanceId.loadOrCreate(dataDirectory),
                    moduleService.getModules());

            CommandManager commands = proxy.getCommandManager();
            CommandMeta meta = commands.metaBuilder("accounts").build();
            commands.register(meta, new MigrateCommand(engine));

            logger.info("Accounts ready: {} module(s) loaded", moduleService.getModules().size());
        } catch (Exception e) {
            logger.error("Accounts failed to start", e);
        }
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (engine != null) {
            engine.close();
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
