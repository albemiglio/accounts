package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;

/** Broadcasts a migration to every other accounts instance (Redis pub/sub in production). */
public interface MigrationPublisher {

    void publish(Task task);
}
