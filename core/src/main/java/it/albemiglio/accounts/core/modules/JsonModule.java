package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Migrates the vanilla server-root JSON stores that key players by UUID — {@code ops.json},
 * {@code whitelist.json}, {@code banned-players.json} and {@code usercache.json}. Each stores the UUID
 * as its lowercase dashed string (what Mojang writes), so migration is a literal swap of that 36-char
 * token to the new one; it cannot collide with any other field. Absent or unmentioning files are left
 * alone. This is server-root data, distinct from the per-world {@code NbtModule}.
 */
public class JsonModule extends Module {

    private static final List<String> VANILLA_FILES = Arrays.asList(
            "ops.json", "whitelist.json", "banned-players.json", "usercache.json");

    private final Path serverDir;

    public JsonModule(String name, Platform platform, Path serverDir) {
        super(name, platform);
        this.serverDir = serverDir;
    }

    @Override
    public void execute(Pair<UUID, UUID> migration) {
        for (String fileName : VANILLA_FILES) {
            rewrite(serverDir.resolve(fileName), migration.getLeft().toString(), migration.getRight().toString());
        }
    }

    private void rewrite(Path file, String oldId, String newId) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String content = Files.readString(file);
            String replaced = content.replace(oldId, newId);
            if (!replaced.equals(content)) {
                Files.writeString(file, replaced);
            }
        } catch (IOException e) {
            throw new MigrationException("JSON migration failed for " + file, e);
        }
    }
}
