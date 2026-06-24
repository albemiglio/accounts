package it.albemiglio.accounts.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.services.BatchService;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * Admin command {@code /accounts migrate <fromUuid> <toUuid> [username]} that enqueues a UUID
 * migration through the engine. The proxy is the natural place to drive a migration that spans the
 * network's databases.
 */
public final class MigrateCommand implements SimpleCommand {

    private static final String USAGE = "Usage: /accounts migrate <fromUuid> <toUuid> [username]";

    private final BatchService batchService;

    public MigrateCommand(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || !args[0].equalsIgnoreCase("migrate")) {
            invocation.source().sendMessage(Component.text(USAGE));
            return;
        }
        try {
            Task task = toTask(Arrays.copyOfRange(args, 1, args.length));
            batchService.handleRequest(task);
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

    static Task toTask(String[] args) {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("expected <fromUuid> <toUuid> [username]");
        }
        UUID from = UUID.fromString(args[0]);
        UUID to = UUID.fromString(args[1]);
        Task task = new Task();
        task.setMigration(Pair.of(from, to));
        task.setUsername(args.length == 3 ? args[2] : "");
        task.setCurrFailures(0);
        return task;
    }
}
