package it.albemiglio.accounts.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceIdTest {

    @Test
    void generatesAndPersistsAnId(@TempDir Path dir) {
        String id = InstanceId.loadOrCreate(dir);

        assertFalse(id.isBlank());
        assertTrue(Files.exists(dir.resolve("instance-id")));
    }

    @Test
    void isStableAcrossRestarts(@TempDir Path dir) {
        String first = InstanceId.loadOrCreate(dir);
        String second = InstanceId.loadOrCreate(dir);

        assertEquals(first, second);
    }
}
