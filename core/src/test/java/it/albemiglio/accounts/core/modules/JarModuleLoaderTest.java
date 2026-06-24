package it.albemiglio.accounts.core.modules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JarModuleLoaderTest {

    @Test
    void discoversModulesFromServiceProvidersOnTheClassLoader() {
        List<Module> modules = JarModuleLoader.fromClassLoader(getClass().getClassLoader());

        assertTrue(modules.stream().anyMatch(m -> "test-provided".equals(m.getName())),
                "expected the SPI-provided module to be discovered");
    }
}
