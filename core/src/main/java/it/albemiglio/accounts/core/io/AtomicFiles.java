package it.albemiglio.accounts.core.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a file so a crash mid-write can never corrupt it. The new content is rendered into a temp file
 * in the same directory, which is then atomically renamed onto the target: a reader (or the next server
 * start) always sees either the whole old file or the whole new one, never a half-written one. This is
 * the pattern Querz's {@code MCAUtil} already uses for region files; the {@code .dat} and {@code .json}
 * stores were instead being truncated in place, so a crash between truncate and flush left them corrupt.
 */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    /** Renders the content into the given temp path (which will then be moved onto the real target). */
    @FunctionalInterface
    public interface Writer {
        void writeTo(Path tmp) throws IOException;
    }

    /**
     * Runs {@code writer} against a fresh temp file next to {@code target}, then atomically moves it onto
     * {@code target}. On any failure the temp file is removed and {@code target} is left untouched.
     * Safe under the module's parallel writes: each call gets its own uniquely-named temp file.
     */
    public static void write(Path target, Writer writer) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Path tmp = Files.createTempFile(dir, target.getFileName().toString(), ".tmp");
        try {
            writer.writeTo(tmp);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Rare filesystem without atomic rename: fall back to a replacing move. Still far safer than
                // truncating in place — the target is only touched once the temp file is fully written.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
    }
}
