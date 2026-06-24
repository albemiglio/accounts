package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;

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

    public BroadcastMigrationService(String instanceId, InstanceMigrator migrator,
                                     MigrationStore store, MigrationPublisher publisher) {
        this.instanceId = instanceId;
        this.migrator = migrator;
        this.store = store;
        this.publisher = publisher;
    }

    /** Entry point for a new migration (admin command or Nyx): record, apply here, broadcast. */
    public void migrate(Task task) {
        store.record(task);
        migrator.apply(task);
        publisher.publish(task);
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
