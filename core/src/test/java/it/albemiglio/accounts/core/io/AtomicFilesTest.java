package it.albemiglio.accounts.core.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicFilesTest {

    @Test
    void writesTheContentAndLeavesNoTempFile(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("level.dat");
        Files.write(target, "old".getBytes(StandardCharsets.UTF_8));

        AtomicFiles.write(target, tmp -> Files.write(tmp, "new".getBytes(StandardCharsets.UTF_8)));

        assertEquals("new", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        assertEquals(List.of("level.dat"), fileNames(dir), "no temp file must be left behind");
    }

    @Test
    void createsTheTargetWhenAbsent(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("whitelist.json");

        AtomicFiles.write(target, tmp -> Files.write(tmp, "[]".getBytes(StandardCharsets.UTF_8)));

        assertEquals("[]", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    void leavesOriginalIntactAndNoTempWhenWriterFails(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("ops.json");
        Files.write(target, "original".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () ->
                AtomicFiles.write(target, tmp -> {
                    throw new IOException("boom");
                }));

        assertEquals("original", new String(Files.readAllBytes(target), StandardCharsets.UTF_8),
                "a failed write must not corrupt the existing target");
        assertEquals(List.of("ops.json"), fileNames(dir), "a failed write must not leak a temp file");
    }

    private static List<String> fileNames(Path dir) throws IOException {
        try (Stream<Path> list = Files.list(dir)) {
            return list.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }
}
