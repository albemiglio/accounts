package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Task;

import java.util.Collection;

/**
 * Applies a migration to one instance's local modules. Idempotent: if the migration is already
 * recorded as applied for this instance it does nothing, so a retry or a duplicate broadcast is safe.
 * Marks the migration applied only when every enabled module succeeded; a partial failure is recorded
 * as failed and left un-applied, so it will be retried (the column relabel itself is idempotent).
 */
public final class InstanceMigrator {

    private final String instanceId;
    private final Collection<Module> modules;
    private final MigrationLog log;

    public InstanceMigrator(String instanceId, Collection<Module> modules, MigrationLog log) {
        this.instanceId = instanceId;
        this.modules = modules;
        this.log = log;
    }

    public void apply(Task task) {
        String id = migrationId(task);
        if (log.hasApplied(id, instanceId)) {
            return;
        }
        boolean anyFailed = false;
        for (Module module : modules) {
            if (!module.isEnabled()) {
                continue;
            }
            try {
                module.execute(task.getMigration());
            } catch (MigrationException e) {
                anyFailed = true;
            }
        }
        if (anyFailed) {
            log.markFailed(id, instanceId);
        } else {
            log.markApplied(id, instanceId);
        }
    }

    public static String migrationId(Task task) {
        return task.getMigration().getLeft() + ">" + task.getMigration().getRight();
    }
}
