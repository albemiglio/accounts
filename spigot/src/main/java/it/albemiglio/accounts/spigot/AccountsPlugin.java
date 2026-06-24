package it.albemiglio.accounts.spigot;

import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.services.AccountsEngine;
import it.albemiglio.accounts.core.services.InstanceId;
import it.albemiglio.accounts.core.services.MigrationArgs;
import it.albemiglio.accounts.core.services.ModuleService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spigot entry point: a backend runs this so its own local databases are migrated when a UUID
 * migration is broadcast. Boots the engine (which subscribes for broadcasts and recovers anything
 * missed while the server was down) and exposes {@code /accounts migrate} for manual use.
 */
public final class AccountsPlugin extends JavaPlugin {

    private AccountsEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        Path dataDir = getDataFolder().toPath();
        Path modulesDir = dataDir.resolve(config.getString("modules-dir", "modules"));
        try {
            Files.createDirectories(modulesDir);
        } catch (IOException e) {
            getLogger().severe("Could not create the modules directory: " + e.getMessage());
            return;
        }

        ModuleService moduleService = new ModuleService(count -> { });
        moduleService.loadModules(modulesDir);
        moduleService.loadJarModules(dataDir.resolve("jar-modules"));

        this.engine = AccountsEngine.start(
                config.getString("redis.host", "localhost"),
                config.getInt("redis.port", 6379),
                config.getString("redis.password", ""),
                InstanceId.loadOrCreate(dataDir),
                moduleService.getModules());

        getLogger().info("Accounts ready: " + moduleService.getModules().size() + " module(s) loaded");
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.close();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("accounts.migrate")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (engine == null) {
            sender.sendMessage("Accounts is not running.");
            return true;
        }
        try {
            Task task = MigrationArgs.parse(args);
            engine.migrate(task);
            sender.sendMessage("Queued migration "
                    + task.getMigration().getLeft() + " -> " + task.getMigration().getRight());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Usage: /accounts migrate <fromUuid> <toUuid> [username]");
        }
        return true;
    }
}
