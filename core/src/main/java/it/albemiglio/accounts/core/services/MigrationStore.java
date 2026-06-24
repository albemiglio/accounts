package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;

import java.util.Collection;

/**
 * Durable home for migrations. Extends {@link MigrationLog} (per-instance apply tracking) with the
 * record of the migrations themselves, so an instance can ask what it still owes after being offline.
 */
public interface MigrationStore extends MigrationLog {

    void record(Task task);

    /** Migrations that have been recorded but not yet applied by this instance. */
    Collection<Task> pending(String instanceId);
}
