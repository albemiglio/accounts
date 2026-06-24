package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.Task;

import java.util.UUID;

/**
 * Parses the {@code migrate <fromUuid> <toUuid> [username]} admin command into a {@link Task}. Shared
 * by every platform's command so the parsing lives (and is tested) in one place.
 */
public final class MigrationArgs {

    private MigrationArgs() {
    }

    public static Task parse(String[] args) {
        if (args.length < 3 || args.length > 4 || !args[0].equalsIgnoreCase("migrate")) {
            throw new IllegalArgumentException("expected: migrate <fromUuid> <toUuid> [username]");
        }
        UUID from = UUID.fromString(args[1]);
        UUID to = UUID.fromString(args[2]);
        Task task = new Task();
        task.setMigration(Pair.of(from, to));
        task.setUsername(args.length == 4 ? args[3] : "");
        task.setCurrFailures(0);
        return task;
    }
}
