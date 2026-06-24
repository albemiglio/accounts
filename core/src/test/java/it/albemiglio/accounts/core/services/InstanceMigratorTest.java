package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.Task;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceMigratorTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    static class RecordingModule extends Module {
        final List<Pair<UUID, UUID>> executed = new ArrayList<>();

        RecordingModule(boolean enabled) {
            super("rec", Platform.SPIGOT, null);
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
            super("fail", Platform.SPIGOT, null);
            enable();
        }

        @Override
        public void execute(Pair<UUID, UUID> migration) {
            throw new MigrationException("boom", null);
        }
    }

    static class ExplodingModule extends Module {
        ExplodingModule() {
            super("explode", Platform.SPIGOT, null);
            enable();
        }

        @Override
        public void execute(Pair<UUID, UUID> migration) {
            throw new RuntimeException("driver pool blew up");
        }
    }

    static class FakeLog implements MigrationLog {
        final Set<String> applied = new HashSet<>();
        final Set<String> failed = new HashSet<>();

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
            failed.add(migrationId + "@" + instanceId);
        }
    }

    private static Task task(UUID oldId, UUID newId) {
        Task t = new Task();
        t.setMigration(Pair.of(oldId, newId));
        t.setUsername("Notch");
        return t;
    }

    @Test
    void appliesEnabledModulesOnceAndRecordsApplied() {
        RecordingModule enabled = new RecordingModule(true);
        RecordingModule disabled = new RecordingModule(false);
        FakeLog log = new FakeLog();
        InstanceMigrator migrator = new InstanceMigrator("inst-1", List.of(enabled, disabled), log);

        migrator.apply(task(OLD, NEW));

        assertEquals(List.of(Pair.of(OLD, NEW)), enabled.executed);
        assertTrue(disabled.executed.isEmpty());
        assertTrue(log.hasApplied(InstanceMigrator.migrationId(task(OLD, NEW)), "inst-1"));
    }

    @Test
    void skipsWhenAlreadyApplied() {
        RecordingModule enabled = new RecordingModule(true);
        FakeLog log = new FakeLog();
        log.markApplied(InstanceMigrator.migrationId(task(OLD, NEW)), "inst-1");
        InstanceMigrator migrator = new InstanceMigrator("inst-1", List.of(enabled), log);

        migrator.apply(task(OLD, NEW));

        assertTrue(enabled.executed.isEmpty());
    }

    @Test
    void recordsFailureAndDoesNotMarkAppliedWhenAModuleFails() {
        FailingModule failing = new FailingModule();
        FakeLog log = new FakeLog();
        InstanceMigrator migrator = new InstanceMigrator("inst-1", List.of(failing), log);

        migrator.apply(task(OLD, NEW));

        String id = InstanceMigrator.migrationId(task(OLD, NEW));
        assertFalse(log.applied.contains(id + "@inst-1"));
        assertTrue(log.failed.contains(id + "@inst-1"));
    }

    @Test
    void recordsFailureInsteadOfCrashingWhenAModuleThrowsUnexpectedly() {
        ExplodingModule exploding = new ExplodingModule();
        FakeLog log = new FakeLog();
        InstanceMigrator migrator = new InstanceMigrator("inst-1", List.of(exploding), log);

        migrator.apply(task(OLD, NEW)); // must not propagate

        String id = InstanceMigrator.migrationId(task(OLD, NEW));
        assertFalse(log.applied.contains(id + "@inst-1"));
        assertTrue(log.failed.contains(id + "@inst-1"));
    }
}
