package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;

public interface TaskQueue {

    Task popFromQueue();

    void addToQueue(Task task);

    long queueSize();
}
