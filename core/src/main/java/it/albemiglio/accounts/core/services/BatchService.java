package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Task;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BatchService {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BATCH_WINDOW_MILLIS = 5000;
    private static final int TTL_EXTENSION_INTERVAL = 10;

    private final Coordinator coordinator;
    private final Collection<Module> modules;
    private final int maxRetries;
    private final long batchWindowMillis;

    private final ConcurrentLinkedQueue<Task> localQueue = new ConcurrentLinkedQueue<>();
    private boolean isProcessing = false;
    private final String instanceId;

    public BatchService(RedisService redisService, Collection<Module> modules) {
        this(redisService, modules, DEFAULT_MAX_RETRIES, DEFAULT_BATCH_WINDOW_MILLIS);
    }

    public BatchService(Coordinator coordinator, Collection<Module> modules, int maxRetries, long batchWindowMillis) {
        this.coordinator = coordinator;
        this.modules = modules;
        this.maxRetries = maxRetries;
        this.batchWindowMillis = batchWindowMillis;
        this.instanceId = coordinator.getInstanceId();
    }

    // future API methods will call this method to handle migrations
    public synchronized void handleRequest(Task task) {
        coordinator.addToQueue(task);

        if (!isProcessing && (coordinator.getLeader() == null || coordinator.getLeader().isEmpty())) {
            if (coordinator.trySetLeader()) {
                startNewBatch();
            }
        }
    }

    private void startNewBatch() {
        isProcessing = true;

        startTTLUpdater();

        new Thread(() -> {
            try {
                // wait some time to let instances fill the common queue a little more
                Thread.sleep(batchWindowMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runBatch();
        }).start();
    }

    void runBatch() {
        while (isLeader()) {
            fillLocalQueueFromRedis();
            while (!localQueue.isEmpty()) {
                process(localQueue.poll());
            }
            if (coordinator.queueSize() == 0) {
                break;
            }
        }
        if (isLeader()) {
            coordinator.clearLeader();
        }
        isProcessing = false;
    }

    private boolean isLeader() {
        return instanceId.equals(coordinator.getLeader());
    }

    private void fillLocalQueueFromRedis() {
        while (coordinator.queueSize() > 0) {
            Task task = coordinator.popFromQueue();
            if (task != null) {
                localQueue.add(task);
            }
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
                coordinator.addToQueue(task);
            }
        }
    }

    private void startTTLUpdater() {
        new Thread(() -> {
            while (isProcessing) {
                try {
                    Thread.sleep(TTL_EXTENSION_INTERVAL * 1000);
                    if (isLeader()) {
                        coordinator.extendLeaderTTL();
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
