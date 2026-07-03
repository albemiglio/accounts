package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleServiceTest {

    private static final String YAML = String.join("\n",
            "name: %s",
            "platform: BUNGEECORD",
            "enabled: true",
            "database:",
            "  type: SQLITE",
            "  database: ':memory:'",
            "replacers: []");

    static class FakeReporter implements ActiveModulesReporter {
        int lastCount = -1;

        @Override
        public void updateActiveModules(int moduleCount) {
            this.lastCount = moduleCount;
        }
    }

    @Test
    void loadModulesRegistersEachByNameAndReportsActiveCount(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("authme.yml"), String.format(YAML, "authme"));
        Files.writeString(dir.resolve("luckperms.yml"), String.format(YAML, "luckperms"));
        FakeReporter reporter = new FakeReporter();
        ModuleService service = new ModuleService(reporter);

        service.loadModules(dir);

        Set<String> names = service.getModules().stream().map(Module::getName).collect(Collectors.toSet());
        assertEquals(Set.of("authme", "luckperms"), names);
        assertEquals(2, reporter.lastCount);
    }

    @Test
    void serverLocalModulesWinOverPluginBundledOnes(@TempDir Path dir) throws Exception {
        Path modulesDir = Files.createDirectories(dir.resolve("modules"));
        Path pluginsDir = Files.createDirectories(dir.resolve("plugins"));
        Files.writeString(modulesDir.resolve("authme.yml"), String.format(YAML, "authme"));
        String bundled = String.join("\n",
                "name: authme",
                "platform: BUNGEECORD",
                "type: file",
                "directory: plugins/AuthMe/userdata",
                "extension: yml",
                "enabled: false");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(pluginsDir.resolve("authme-plugin.jar")))) {
            out.putNextEntry(new ZipEntry("accounts-module.yml"));
            out.write(bundled.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        ModuleService service = new ModuleService(new FakeReporter());

        service.loadModules(modulesDir);
        service.loadPluginJarModules(pluginsDir);

        assertEquals(1, service.getModules().size());
        assertTrue(service.getModules().iterator().next().isEnabled(),
                "expected the server-local module to survive, not the bundled one");
    }
}
