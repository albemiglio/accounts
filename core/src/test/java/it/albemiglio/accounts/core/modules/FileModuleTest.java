package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileModuleTest {

    private static final UUID OLD = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID NEW = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void renamesTheUuidNamedFileKeepingItsContents(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(OLD + ".yml"), "money: 100");
        FileModule module = new FileModule("essentials", Platform.SPIGOT, dir, "yml");

        module.execute(Pair.of(OLD, NEW));

        assertFalse(Files.exists(dir.resolve(OLD + ".yml")));
        assertTrue(Files.exists(dir.resolve(NEW + ".yml")));
        assertEquals("money: 100", Files.readString(dir.resolve(NEW + ".yml")));
    }

    @Test
    void isANoOpWhenTheOldFileIsAbsent(@TempDir Path dir) {
        FileModule module = new FileModule("essentials", Platform.SPIGOT, dir, "yml");

        module.execute(Pair.of(OLD, NEW)); // must not throw

        assertFalse(Files.exists(dir.resolve(NEW + ".yml")));
    }
}
