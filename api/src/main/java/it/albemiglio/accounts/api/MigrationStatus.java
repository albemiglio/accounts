package it.albemiglio.accounts.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Where one migration has got to across the network: who was expected to apply it, who has, and
 * therefore who is still holding it open. This is the whole answer to "what is happening right now" —
 * an admin view or a caller deciding whether to let a player back in reads it instead of guessing from
 * a timer.
 *
 * <p>An empty {@link #expected()} means nobody ever opened the barrier for this migration, which is
 * not the same as "done": {@link #complete()} stays false so a missing barrier can never read as
 * success.
 */
public final class MigrationStatus {

    private final UUID from;
    private final UUID to;
    private final String username;
    private final Set<String> expected;
    private final Set<String> applied;

    public MigrationStatus(UUID from, UUID to, String username, Set<String> expected, Set<String> applied) {
        this.from = from;
        this.to = to;
        this.username = username == null ? "" : username;
        this.expected = Collections.unmodifiableSet(new LinkedHashSet<>(expected));
        this.applied = Collections.unmodifiableSet(new LinkedHashSet<>(applied));
    }

    public UUID from() {
        return from;
    }

    public UUID to() {
        return to;
    }

    /** The player the migration is for; metadata only, may be empty on older records. */
    public String username() {
        return username;
    }

    /** The instances that must apply this migration, snapshotted when it started. */
    public Set<String> expected() {
        return expected;
    }

    /** The instances that have applied it. */
    public Set<String> applied() {
        return applied;
    }

    /** The instances still holding the migration open — what an admin actually wants to see. */
    public Set<String> waitingOn() {
        Set<String> waiting = new LinkedHashSet<>(expected);
        waiting.removeAll(applied);
        return Collections.unmodifiableSet(waiting);
    }

    public boolean complete() {
        return !expected.isEmpty() && applied.containsAll(expected);
    }

    /** Started and not finished. False when no barrier was ever opened — unknown is not in progress. */
    public boolean inProgress() {
        return !expected.isEmpty() && !complete();
    }

    @Override
    public String toString() {
        return from + " -> " + to + " (" + username + ") " + applied.size() + "/" + expected.size()
                + (complete() ? " complete" : " waiting on " + waitingOn());
    }
}
