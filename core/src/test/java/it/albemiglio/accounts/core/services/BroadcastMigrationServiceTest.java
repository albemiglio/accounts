package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadcastMigrationServiceTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    static class RecordingModule extends Module {
        final List<Pair<UUID, UUID>> executed = new ArrayList<>();

        RecordingModule() {
            super("rec", Platform.SPIGOT, null);
            enable();
        }

        @Override
        public void execute(Pair<UUID, UUID> migration) {
            executed.add(migration);
        }
    }

    static class FakeStore implements MigrationStore {
        final Map<String, Task> recorded = new HashMap<>();
        final Set<String> applied = new HashSet<>();

        @Override
        public void record(Task task) {
            recorded.put(InstanceMigrator.migrationId(task), task);
        }

        @Override
        public Collection<Task> pending(String instanceId) {
            List<Task> out = new ArrayList<>();
            for (Map.Entry<String, Task> e : recorded.entrySet()) {
                if (!applied.contains(e.getKey() + "@" + instanceId)) {
                    out.add(e.getValue());
                }
            }
            return out;
        }

        @Override
        public boolean hasApplied(String migrationId, String instanceId) {
            return applied.contains(migrationId + "@" + instanceId);
        }

        @Override
        public void markApplied(String migrationId, String instanceId) {
            applied.add(migrationId + "@" + instanceId);
        }

        @Override
        public void markFailed(String migrationId, String instanceId) {
        }
    }

    static class RecordingPublisher implements MigrationPublisher {
        final List<Task> published = new ArrayList<>();

        @Override
        public void publish(Task task) {
            published.add(task);
        }
    }

    private static Task task(UUID oldId, UUID newId) {
        Task t = new Task();
        t.setMigration(Pair.of(oldId, newId));
        t.setUsername("Notch");
        return t;
    }

    private static BroadcastMigrationService service(RecordingModule module, FakeStore store, RecordingPublisher pub) {
        InstanceMigrator migrator = new InstanceMigrator("inst-1", List.of(module), store);
        return new BroadcastMigrationService("inst-1", migrator, store, pub);
    }

    @Test
    void migrateRecordsAppliesLocallyAndPublishes() {
        RecordingModule module = new RecordingModule();
        FakeStore store = new FakeStore();
        RecordingPublisher pub = new RecordingPublisher();

        service(module, store, pub).migrate(task(OLD, NEW));

        assertTrue(store.recorded.containsKey(InstanceMigrator.migrationId(task(OLD, NEW))));
        assertEquals(List.of(Pair.of(OLD, NEW)), module.executed);
        assertEquals(1, pub.published.size());
    }

    @Test
    void handleAppliesAReceivedMigration() {
        RecordingModule module = new RecordingModule();
        FakeStore store = new FakeStore();
        RecordingPublisher pub = new RecordingPublisher();

        service(module, store, pub).handle(task(OLD, NEW));

        assertEquals(List.of(Pair.of(OLD, NEW)), module.executed);
    }

    @Test
    void handleAlsoRecordsSoOfflineInstancesCanCatchUp() {
        RecordingModule module = new RecordingModule();
        FakeStore store = new FakeStore();
        RecordingPublisher pub = new RecordingPublisher();

        service(module, store, pub).handle(task(OLD, NEW));

        assertTrue(store.recorded.containsKey(InstanceMigrator.migrationId(task(OLD, NEW))));
    }

    @Test
    void recoverPendingAppliesEverythingThisInstanceStillOwes() {
        RecordingModule module = new RecordingModule();
        FakeStore store = new FakeStore();
        RecordingPublisher pub = new RecordingPublisher();
        store.record(task(OLD, NEW));

        service(module, store, pub).recoverPending();

        assertEquals(List.of(Pair.of(OLD, NEW)), module.executed);
    }
}
