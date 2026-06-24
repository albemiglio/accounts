package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A module for plugins that store a player's data in a file named by their UUID (e.g. EssentialsX's
 * {@code userdata/<uuid>.yml}). Migration is a rename old-uuid -> new-uuid; the contents are left
 * untouched because the file name is the key. Each instance renames its own local file, which is
 * exactly what the broadcast model wants — the backend that owns the file migrates it.
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
        Path oldFile = directory.resolve(migration.getLeft() + "." + extension);
        Path newFile = directory.resolve(migration.getRight() + "." + extension);
        if (!Files.exists(oldFile)) {
            return;
        }
        try {
            Files.move(oldFile, newFile);
        } catch (IOException e) {
            throw new MigrationException("File migration failed for module " + getName(), e);
        }
    }
}
