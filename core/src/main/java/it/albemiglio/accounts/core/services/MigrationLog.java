package it.albemiglio.accounts.core.services;

/**
 * Durable record of which instance has applied which migration. It is what makes a broadcast
 * migration reliable: an instance that was offline during the broadcast can, on restart, see what it
 * still owes and apply it, and a migration that only landed on some instances is visible rather than
 * silently half-done.
 */
public interface MigrationLog {

    boolean hasApplied(String migrationId, String instanceId);

    void markApplied(String migrationId, String instanceId);

    void markFailed(String migrationId, String instanceId);
}
