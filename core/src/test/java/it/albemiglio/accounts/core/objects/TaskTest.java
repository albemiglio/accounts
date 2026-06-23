package it.albemiglio.accounts.core.objects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    private static Task task(UUID oldId, UUID newId, String username, int failures) {
        Task t = new Task();
        t.setMigration(Pair.of(oldId, newId));
        t.setUsername(username);
        t.setCurrFailures(failures);
        return t;
    }

    @Test
    void toStringJoinsFieldsWithSemicolons() {
        Task t = task(OLD, NEW, "Notch", 3);
        assertEquals(OLD + ";" + NEW + ";Notch;3", t.toString());
    }

    @Test
    void fromStringParsesAllFields() {
        Task t = Task.fromString(OLD + ";" + NEW + ";Notch;3");
        assertEquals(OLD, t.getMigration().getLeft());
        assertEquals(NEW, t.getMigration().getRight());
        assertEquals("Notch", t.getUsername());
        assertEquals(3, t.getCurrFailures());
    }

    @Test
    void toStringFromStringRoundTrips() {
        Task original = task(OLD, NEW, "Notch", 3);
        Task restored = Task.fromString(original.toString());
        assertEquals(original, restored);
        assertEquals(original.getCurrFailures(), restored.getCurrFailures());
    }

    @Test
    void equalsIgnoresCurrFailures() {
        assertEquals(task(OLD, NEW, "Notch", 0), task(OLD, NEW, "Notch", 9));
    }
}
