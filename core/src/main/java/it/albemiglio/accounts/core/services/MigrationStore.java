package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;

import java.util.Collection;
import java.util.Set;

/**
 * Durable home for migrations. Extends {@link MigrationLog} (per-instance apply tracking) with the
 * record of the migrations themselves, plus the completion barrier: the set of instances expected to
 * apply a migration (snapshot at initiation) and the set that actually have, so Nyx can tell when a
 * migration is fully done across the network before it lets the player back in.
 */
public interface MigrationStore extends MigrationLog {

    void record(Task task);

    /** Migrations that have been recorded but not yet applied by this instance. */
    Collection<Task> pending(String instanceId);

    /** Records the instances that must apply this migration for it to be considered complete. */
    void recordExpected(String migrationId, Set<String> instances);

    Set<String> expectedInstances(String migrationId);

    Set<String> appliedInstances(String migrationId);
}
