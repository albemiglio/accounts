package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchServiceTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    static class RecordingModule extends Module {
        final List<Pair<UUID, UUID>> executed = new ArrayList<>();

        RecordingModule(boolean enabled) {
            super("rec", Platform.BUNGEECORD, null);
            if (enabled) {
                enable();
            }
        }

        @Override
        public void execute(Pair<UUID, UUID> migration) {
            executed.add(migration);
        }
    }

    static class FailingModule extends Module {
        FailingModule() {
            super("fail", Platform.BUNGEECORD, null);
            enable();
        }

        @Override
        public void execute(Pair<UUID, UUID> migration) {
            throw new MigrationException("boom", null);
        }
    }

    static class FakeQueue implements TaskQueue {
        final Deque<Task> queue = new ArrayDeque<>();

        @Override
        public Task popFromQueue() {
            return queue.poll();
        }

        @Override
        public void addToQueue(Task task) {
            queue.add(task);
        }

        @Override
        public long queueSize() {
            return queue.size();
        }
    }

    private static Task task(UUID oldId, UUID newId, String username) {
        Task t = new Task();
        t.setMigration(Pair.of(oldId, newId));
        t.setUsername(username);
        return t;
    }

    private static BatchService batchService(TaskQueue queue, List<Module> modules, int maxRetries) {
        return new BatchService(new RedisService(), queue, modules, maxRetries);
    }

    @Test
    void processRunsMigrationThroughEnabledModulesOnly() {
        RecordingModule enabled = new RecordingModule(true);
        RecordingModule disabled = new RecordingModule(false);
        BatchService service = batchService(new FakeQueue(), List.of(enabled, disabled), 3);

        service.process(task(OLD, NEW, "Notch"));

        assertEquals(List.of(Pair.of(OLD, NEW)), enabled.executed);
        assertTrue(disabled.executed.isEmpty());
    }

    @Test
    void processRequeuesTaskWithIncrementedFailuresWhenAModuleFails() {
        FakeQueue queue = new FakeQueue();
        BatchService service = batchService(queue, List.of(new FailingModule()), 3);

        service.process(task(OLD, NEW, "Notch"));

        assertEquals(1, queue.queueSize());
        assertEquals(1, queue.popFromQueue().getCurrFailures());
    }

    @Test
    void processDropsTaskWhenRetriesAreExhausted() {
        FakeQueue queue = new FakeQueue();
        BatchService service = batchService(queue, List.of(new FailingModule()), 3);
        Task exhausted = task(OLD, NEW, "Notch");
        exhausted.setCurrFailures(2);

        service.process(exhausted);

        assertEquals(0, queue.queueSize());
    }
}
