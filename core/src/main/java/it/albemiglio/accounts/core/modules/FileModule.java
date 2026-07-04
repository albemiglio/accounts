package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A module for plugins that store a player's data in a file named by their UUID (e.g. EssentialsX's
 * {@code userdata/<uuid>.yml}). Migration is a rename old-uuid -> new-uuid; the contents are left
 * untouched because the file name is the key. An EMPTY extension matches the bare uuid — that covers
 * both extensionless files (GriefDefender's playerdata) and whole per-player DIRECTORIES (FAWE's
 * {@code history/<world>/<uuid>/}), since a move renames either. Each instance renames its own local
 * file, which is exactly what the broadcast model wants — the backend that owns the file migrates it.
 */
public class FileModule extends Module {

    private final Path directory;
    private final String extension;

    public FileModule(String name, Platform platform, Path directory, String extension) {
        super(name, platform);
        this.directory = directory;
        this.extension = extension;
    }

    @Override
    public void execute(Pair<UUID, UUID> migration) {
        String suffix = extension.isEmpty() ? "" : "." + extension;
        Path oldFile = directory.resolve(migration.getLeft() + suffix);
        Path newFile = directory.resolve(migration.getRight() + suffix);
        if (!Files.exists(oldFile)) {
            return;
        }
        try {
            Files.move(oldFile, newFile);
        } catch (IOException e) {
            throw new MigrationException("File migration failed for module " + getName(), e);
        }
    }

    @Override
    public List<Diagnosis> diagnose(UUID probe) {
        String suffix = extension.isEmpty() ? "" : "." + extension;
        Path target = directory.resolve(probe + suffix);
        if (Files.exists(target)) {
            return Collections.singletonList(Diagnosis.verified(getName(), target.toString(), 1));
        }
        return Collections.singletonList(
                Diagnosis.notFound(getName(), directory.resolve("<uuid>" + suffix).toString()));
    }
}
