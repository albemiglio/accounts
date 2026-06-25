package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;

import java.util.Set;

/**
 * Drives migrations in the broadcast model: every accounts instance runs one of these. Initiating a
 * migration records it durably, applies it locally, and broadcasts it; receiving one applies it; and
 * on startup {@link #recoverPending()} replays anything this instance missed while it was down. Apply
 * is idempotent, so the overlap between broadcast delivery and recovery is harmless.
 */
public final class BroadcastMigrationService {

    private final String instanceId;
    private final InstanceMigrator migrator;
    private final MigrationStore store;
    private final MigrationPublisher publisher;
    private final InstanceRegistry registry;

    public BroadcastMigrationService(String instanceId, InstanceMigrator migrator, MigrationStore store,
                                     MigrationPublisher publisher, InstanceRegistry registry) {
        this.instanceId = instanceId;
        this.migrator = migrator;
        this.store = store;
        this.publisher = publisher;
        this.registry = registry;
    }

    /**
     * Entry point for a new migration (admin command or Nyx): snapshot who must apply it (the barrier),
     * record it, apply it here, and broadcast it to the rest.
     */
    public void migrate(Task task) {
        store.recordExpected(InstanceMigrator.migrationId(task), registry.activeInstances());
        store.record(task);
        migrator.apply(task);
        publisher.publish(task);
    }

    /** Whether every instance expected to apply this migration has done so (Nyx's unlock gate). */
    public boolean isComplete(String migrationId) {
        Set<String> expected = store.expectedInstances(migrationId);
        return !expected.isEmpty() && store.appliedInstances(migrationId).containsAll(expected);
    }

    /** Whether this migration was started but isn't done yet — Nyx blocks the player's login while true. */
    public boolean isInProgress(String migrationId) {
        Set<String> expected = store.expectedInstances(migrationId);
        return !expected.isEmpty() && !store.appliedInstances(migrationId).containsAll(expected);
    }

    /**
     * A migration broadcast by another instance (or by Nyx) arrived. Records it too, so durability
     * does not depend on who published it: any online instance persists it for the offline ones.
     */
    public void handle(Task task) {
        store.record(task);
        migrator.apply(task);
    }

    /** Apply everything this instance still owes (called on startup, idempotent). */
    public void recoverPending() {
        for (Task task : store.pending(instanceId)) {
            migrator.apply(task);
        }
    }
}
