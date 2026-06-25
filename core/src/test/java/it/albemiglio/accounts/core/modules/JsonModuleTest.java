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

class JsonModuleTest {

    private static final UUID OLD = UUID.fromString("069a79f4-44e9-4726-a5be-fc65c822c0a9");
    private static final UUID NEW = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");

    @Test
    void rewritesTheUuidInEveryVanillaJsonLeavingTheRestUntouched(@TempDir Path serverDir) throws IOException {
        Files.writeString(serverDir.resolve("usercache.json"),
                "[{\"name\":\"Steve\",\"uuid\":\"" + OLD + "\",\"expiresOn\":\"2099-01-01 00:00:00 +0000\"}]");
        Files.writeString(serverDir.resolve("ops.json"),
                "[{\"uuid\":\"" + OLD + "\",\"name\":\"Steve\",\"level\":4}]");

        new JsonModule("vanilla-json", Platform.SPIGOT, serverDir).execute(Pair.of(OLD, NEW));

        String usercache = Files.readString(serverDir.resolve("usercache.json"));
        assertTrue(usercache.contains(NEW.toString()));
        assertFalse(usercache.contains(OLD.toString()));
        assertTrue(usercache.contains("\"name\":\"Steve\""));
        assertTrue(usercache.contains("expiresOn"));
        assertTrue(Files.readString(serverDir.resolve("ops.json")).contains(NEW.toString()));
    }

    @Test
    void isANoOpWhenAFileIsAbsentOrDoesNotMentionThePlayer(@TempDir Path serverDir) throws IOException {
        // only whitelist exists, and it lists someone else
        UUID other = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        Files.writeString(serverDir.resolve("whitelist.json"),
                "[{\"uuid\":\"" + other + "\",\"name\":\"Alex\"}]");

        new JsonModule("vanilla-json", Platform.SPIGOT, serverDir).execute(Pair.of(OLD, NEW)); // must not throw

        assertEquals("[{\"uuid\":\"" + other + "\",\"name\":\"Alex\"}]",
                Files.readString(serverDir.resolve("whitelist.json")));
        assertFalse(Files.exists(serverDir.resolve("ops.json")));
    }
}
