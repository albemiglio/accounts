package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.objects.Task;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MigrationArgsTest {

    private static final UUID FROM = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID TO = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void parsesMigrateWithUsername() {
        Task task = MigrationArgs.parse(new String[]{"migrate", FROM.toString(), TO.toString(), "Notch"});

        assertEquals(FROM, task.getMigration().getLeft());
        assertEquals(TO, task.getMigration().getRight());
        assertEquals("Notch", task.getUsername());
        assertEquals(0, task.getCurrFailures());
    }

    @Test
    void defaultsUsernameToEmpty() {
        Task task = MigrationArgs.parse(new String[]{"migrate", FROM.toString(), TO.toString()});

        assertEquals("", task.getUsername());
    }

    @Test
    void rejectsTooFewArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> MigrationArgs.parse(new String[]{"migrate", FROM.toString()}));
    }

    @Test
    void rejectsUnknownSubcommand() {
        assertThrows(IllegalArgumentException.class,
                () -> MigrationArgs.parse(new String[]{"foo", FROM.toString(), TO.toString()}));
    }
}
