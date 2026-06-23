package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Task;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TransferService implements IService {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private final Set<Task> tasks;
    private TaskQueue queue;
    private Collection<Module> modules;
    private int maxRetries;

    public TransferService() {
        this.tasks = new HashSet<>();
    }

    public TransferService(TaskQueue queue, Collection<Module> modules) {
        this(queue, modules, DEFAULT_MAX_RETRIES);
    }

    public TransferService(TaskQueue queue, Collection<Module> modules, int maxRetries) {
        this.tasks = new HashSet<>();
        this.queue = queue;
        this.modules = modules;
        this.maxRetries = maxRetries;
    }

    public void drain() {
        while (queue.queueSize() > 0) {
            process(queue.popFromQueue());
        }
    }

    public void process(Task task) {
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
            task.setCurrFailures(task.getCurrFailures() + 1);
            if (task.getCurrFailures() < maxRetries) {
                queue.addToQueue(task);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void end() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
