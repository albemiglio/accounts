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

class TransferServiceTest {

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

    @Test
    void runsMigrationThroughEnabledModulesOnly() {
        RecordingModule enabled = new RecordingModule(true);
        RecordingModule disabled = new RecordingModule(false);
        TransferService service = new TransferService(new FakeQueue(), List.of(enabled, disabled));

        service.process(task(OLD, NEW, "Notch"));

        assertEquals(List.of(Pair.of(OLD, NEW)), enabled.executed);
        assertTrue(disabled.executed.isEmpty());
    }

    @Test
    void requeuesTaskWithIncrementedFailuresWhenAModuleFails() {
        FakeQueue queue = new FakeQueue();
        TransferService service = new TransferService(queue, List.of(new FailingModule()), 3);

        service.process(task(OLD, NEW, "Notch"));

        assertEquals(1, queue.queueSize());
        assertEquals(1, queue.popFromQueue().getCurrFailures());
    }

    @Test
    void drainProcessesEveryQueuedTask() {
        FakeQueue queue = new FakeQueue();
        queue.addToQueue(task(OLD, NEW, "Notch"));
        queue.addToQueue(task(new UUID(0L, 3L), new UUID(0L, 4L), "Jeb"));
        RecordingModule module = new RecordingModule(true);
        TransferService service = new TransferService(queue, List.of(module));

        service.drain();

        assertEquals(2, module.executed.size());
        assertEquals(0, queue.queueSize());
    }

    @Test
    void dropsTaskWhenRetriesAreExhausted() {
        FakeQueue queue = new FakeQueue();
        TransferService service = new TransferService(queue, List.of(new FailingModule()), 3);
        Task exhausted = task(OLD, NEW, "Notch");
        exhausted.setCurrFailures(2); // the next failure reaches the ceiling of 3

        service.process(exhausted);

        assertEquals(0, queue.queueSize());
    }
}
