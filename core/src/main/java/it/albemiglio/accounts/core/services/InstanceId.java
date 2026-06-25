package it.albemiglio.accounts.core.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A per-installation identity that survives restarts. The broadcast catch-up needs it: an instance
 * recognizes its own past applies only if its id is the same after a reboot, so a random per-boot id
 * (the old {@code instance-<timestamp>}) would make every restart look like a brand-new instance.
 */
public final class InstanceId {

    private InstanceId() {
    }

    public static String loadOrCreate(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
            Path file = dataDirectory.resolve("instance-id");
            if (Files.exists(file)) {
                String existing = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                if (!existing.isEmpty()) {
                    return existing;
                }
            }
            String id = UUID.randomUUID().toString();
            Files.write(file, id.getBytes(StandardCharsets.UTF_8));
            return id;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read or create the accounts instance id", e);
        }
    }
}
