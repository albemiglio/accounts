package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.objects.enums.Platform;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/** A stand-in for a community jar's provider, declared in test META-INF/services. */
public class TestModuleProvider implements ModuleProvider {

    @Override
    public Collection<Module> modules() {
        return List.of(new FileModule("test-provided", Platform.SPIGOT, Path.of("."), "yml"));
    }
}
