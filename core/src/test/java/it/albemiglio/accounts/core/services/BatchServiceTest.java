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
import static org.junit.jupiter.api.Assertions.assertNull;
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
        int executions = 0;

        FailingModule() {
            super("fail", Platform.BUNGEECORD, null);
            enable();
        }

        @Override
        public void execute(Pair<UUID, UUID> migration) {
            executions++;
            throw new MigrationException("boom", null);
        }
    }

    static class FakeCoordinator implements Coordinator {
        final Deque<Task> queue = new ArrayDeque<>();
        String leader;

        @Override
        public String getInstanceId() {
            return "test-instance";
        }

        @Override
        public boolean trySetLeader() {
            if (leader == null) {
                leader = getInstanceId();
                return true;
            }
            return false;
        }

        @Override
        public String getLeader() {
            return leader;
        }

        @Override
        public void clearLeader() {
            leader = null;
        }

        @Override
        public void extendLeaderTTL() {
        }

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

    private static BatchService batchService(Coordinator coordinator, List<Module> modules, int maxRetries) {
        return new BatchService(coordinator, modules, maxRetries, 0L);
    }

    @Test
    void processRunsMigrationThroughEnabledModulesOnly() {
        RecordingModule enabled = new RecordingModule(true);
        RecordingModule disabled = new RecordingModule(false);
        BatchService service = batchService(new FakeCoordinator(), List.of(enabled, disabled), 3);

        service.process(task(OLD, NEW, "Notch"));

        assertEquals(List.of(Pair.of(OLD, NEW)), enabled.executed);
        assertTrue(disabled.executed.isEmpty());
    }

    @Test
    void processRequeuesTaskWithIncrementedFailuresWhenAModuleFails() {
        FakeCoordinator coordinator = new FakeCoordinator();
        BatchService service = batchService(coordinator, List.of(new FailingModule()), 3);

        service.process(task(OLD, NEW, "Notch"));

        assertEquals(1, coordinator.queueSize());
        assertEquals(1, coordinator.popFromQueue().getCurrFailures());
    }

    @Test
    void processDropsTaskWhenRetriesAreExhausted() {
        FakeCoordinator coordinator = new FakeCoordinator();
        BatchService service = batchService(coordinator, List.of(new FailingModule()), 3);
        Task exhausted = task(OLD, NEW, "Notch");
        exhausted.setCurrFailures(2);

        service.process(exhausted);

        assertEquals(0, coordinator.queueSize());
    }

    @Test
    void runBatchMigratesEveryQueuedTaskThenReleasesLeadership() {
        FakeCoordinator coordinator = new FakeCoordinator();
        coordinator.trySetLeader();
        coordinator.addToQueue(task(OLD, NEW, "Notch"));
        coordinator.addToQueue(task(new UUID(0L, 3L), new UUID(0L, 4L), "Jeb"));
        RecordingModule module = new RecordingModule(true);
        BatchService service = batchService(coordinator, List.of(module), 3);

        service.runBatch();

        assertEquals(2, module.executed.size());
        assertEquals(0, coordinator.queueSize());
        assertNull(coordinator.getLeader());
    }

    @Test
    void runBatchRetriesUpToCeilingThenReleasesLeadership() {
        FakeCoordinator coordinator = new FakeCoordinator();
        coordinator.trySetLeader();
        coordinator.addToQueue(task(OLD, NEW, "Notch"));
        FailingModule module = new FailingModule();
        BatchService service = batchService(coordinator, List.of(module), 3);

        service.runBatch();

        assertEquals(3, module.executions);
        assertEquals(0, coordinator.queueSize());
        assertNull(coordinator.getLeader());
    }
}
