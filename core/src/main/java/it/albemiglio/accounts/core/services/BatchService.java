package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Task;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BatchService {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int TTL_EXTENSION_INTERVAL = 10;

    private final RedisService redisService;
    private final TaskQueue queue;
    private final Collection<Module> modules;
    private final int maxRetries;

    private final ConcurrentLinkedQueue<Task> localQueue = new ConcurrentLinkedQueue<>();
    private boolean isProcessing = false;
    private final String instanceId;

    public BatchService(RedisService redisService, Collection<Module> modules) {
        this(redisService, redisService, modules, DEFAULT_MAX_RETRIES);
    }

    public BatchService(RedisService redisService, TaskQueue queue, Collection<Module> modules, int maxRetries) {
        this.redisService = redisService;
        this.queue = queue;
        this.modules = modules;
        this.maxRetries = maxRetries;
        this.instanceId = redisService.getInstanceId();
    }

    // future API methods will call this method to handle migrations
    public synchronized void handleRequest(Task task) {
        redisService.addToQueue(task);

        if (!isProcessing && (redisService.getLeader() == null || redisService.getLeader().isEmpty())) {
            if (redisService.trySetLeader()) {
                startNewBatch();
            }
        }
    }

    private void startNewBatch() {
        isProcessing = true;

        startTTLUpdater();

        new Thread(() -> {
            try {
                // wait some seconds to allow instances to fill the common queue for a little more
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (instanceId.equals(redisService.getLeader())) {
                fillLocalQueueFromRedis();
                processBatch();
            } else {
                // no more leader -> stop processing
                isProcessing = false;
            }
        }).start();
    }

    private void fillLocalQueueFromRedis() {
        while (redisService.queueSize() > 0) {
            Task task = redisService.popFromQueue();
            if (task != null) {
                localQueue.add(task);
            }
        }
    }

    private void processBatch() {
        while (!localQueue.isEmpty()) {
            Task task = localQueue.poll();
            process(task);
        }


        // reschedule failed operations from redis service, reverting them and putting them on redis queue

        if (redisService.queueSize() > 0 && instanceId.equals(redisService.getLeader())) {
            // restart processing in case of new tasks (or previous reverted ones)
            startNewBatch();
        }
        else {
            redisService.clearLeader();
            isProcessing = false;
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

    private void startTTLUpdater() {
        new Thread(() -> {
            while (isProcessing) {
                try {
                    Thread.sleep(TTL_EXTENSION_INTERVAL * 1000);
                    if (instanceId.equals(redisService.getLeader())) {
                        redisService.extendLeaderTTL();
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
