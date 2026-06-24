package it.albemiglio.accounts.bungee;

import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.services.AccountsEngine;
import it.albemiglio.accounts.core.services.MigrationArgs;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

/** {@code /accounts migrate <fromUuid> <toUuid> [username]} on BungeeCord. */
public final class MigrateCommand extends Command {

    private static final String USAGE = "Usage: /accounts migrate <fromUuid> <toUuid> [username]";

    private final AccountsEngine engine;

    public MigrateCommand(AccountsEngine engine) {
        super("accounts", "accounts.migrate");
        this.engine = engine;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            Task task = MigrationArgs.parse(args);
            engine.migrate(task);
            sender.sendMessage(new TextComponent(
                    "Queued migration " + task.getMigration().getLeft() + " -> " + task.getMigration().getRight()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(new TextComponent(USAGE));
        }
    }
}
