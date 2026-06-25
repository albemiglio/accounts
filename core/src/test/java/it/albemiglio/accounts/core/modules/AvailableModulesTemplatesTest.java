package it.albemiglio.accounts.core.modules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the shipped example configs in available-modules/ so a typo can't ship a broken template. */
class AvailableModulesTemplatesTest {

    @Test
    void everyShippedTemplateParsesAndBuilds() {
        Path templates = Paths.get("..", "available-modules");
        assertTrue(Files.isDirectory(templates), "available-modules/ should sit at the repo root");

        List<Module> modules = new YamlModuleLoader().load(templates); // builds each via the real factory

        assertFalse(modules.isEmpty(), "no templates were loaded");
        assertTrue(modules.stream().allMatch(Module::isEnabled), "templates ship enabled: true");
    }
}
