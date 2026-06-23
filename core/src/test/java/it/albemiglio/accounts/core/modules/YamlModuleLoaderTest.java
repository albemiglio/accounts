package it.albemiglio.accounts.core.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlModuleLoaderTest {

    private static final String YAML = String.join("\n",
            "name: %s",
            "platform: BUNGEECORD",
            "enabled: true",
            "database:",
            "  type: SQLITE",
            "  database: ':memory:'",
            "replacers:",
            "  - table: %s",
            "    column: uuid");

    @Test
    void loadsEveryYamlFileAndIgnoresOthers(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("authme.yml"), String.format(YAML, "authme", "authme"));
        Files.writeString(dir.resolve("luckperms.yaml"), String.format(YAML, "luckperms", "lp_players"));
        Files.writeString(dir.resolve("notes.txt"), "not a module");

        List<Module> modules = new YamlModuleLoader().load(dir);

        assertEquals(2, modules.size());
        assertTrue(modules.stream().allMatch(Module::isEnabled));
    }

    @Test
    void returnsEmptyListWhenDirectoryDoesNotExist(@TempDir Path dir) {
        List<Module> modules = new YamlModuleLoader().load(dir.resolve("missing"));
        assertEquals(List.of(), modules);
    }
}
