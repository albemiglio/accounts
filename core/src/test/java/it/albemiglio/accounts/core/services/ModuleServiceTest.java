package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
