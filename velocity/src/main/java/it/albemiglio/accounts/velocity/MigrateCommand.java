package it.albemiglio.accounts.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.services.AccountsEngine;
import it.albemiglio.accounts.core.services.MigrationArgs;
import net.kyori.adventure.text.Component;

/**
 * Admin command {@code /accounts migrate <fromUuid> <toUuid> [username]} that broadcasts a UUID
 * migration through the engine. The proxy is the natural place to drive a network-wide migration.
 */
public final class MigrateCommand implements SimpleCommand {

    private static final String USAGE = "Usage: /accounts migrate <fromUuid> <toUuid> [username]";

    private final AccountsEngine engine;

    public MigrateCommand(AccountsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(Invocation invocation) {
        try {
            Task task = MigrationArgs.parse(invocation.arguments());
            engine.migrate(task);
            invocation.source().sendMessage(Component.text(
                    "Queued migration " + task.getMigration().getLeft() + " -> " + task.getMigration().getRight()));
        } catch (IllegalArgumentException e) {
            invocation.source().sendMessage(Component.text(USAGE));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("accounts.migrate");
    }
}
