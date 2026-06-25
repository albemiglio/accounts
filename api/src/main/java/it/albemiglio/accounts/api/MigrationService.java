package it.albemiglio.accounts.api;

import java.util.UUID;

/**
 * The contract another plugin on the same proxy (e.g. Nyx) uses to trigger a migration in-process.
 * accounts' proxy plugin implements this; a caller looks the plugin up and calls {@link #migrate}.
 * Ship this jar as a {@code provided} dependency in the caller — never shade it, or the two copies
 * would have different classloader identity and the cast would fail.
 */
public interface MigrationService {

    /**
     * Migrate all data keyed by {@code from} to {@code to} across the network, atomically per
     * database and reliably (durable, idempotent, with offline catch-up). {@code username} is
     * metadata for the migration record.
     */
    void migrate(UUID from, UUID to, String username);

    /**
     * Whether a migration {@code from -> to} is started but not yet finished across the network. Nyx
     * blocks the player's premium login while this is true, so they can't re-enter mid-transfer.
     */
    boolean isMigrationInProgress(UUID from, UUID to);
}
