package it.albemiglio.accounts.core.services;

import it.albemiglio.accounts.core.modules.JarModuleLoader;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.modules.PluginJarModuleLoader;
import it.albemiglio.accounts.core.modules.YamlModuleLoader;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleService {

    private final ConcurrentHashMap<String, Module> modules;
    private final ActiveModulesReporter reporter;
    private final YamlModuleLoader loader;

    public ModuleService(ActiveModulesReporter reporter) {
        this(reporter, new YamlModuleLoader());
    }

    public ModuleService(ActiveModulesReporter reporter, YamlModuleLoader loader) {
        this.modules = new ConcurrentHashMap<>();
        this.reporter = reporter;
        this.loader = loader;
    }

    public void loadModules(Path directory) {
        for (Module module : loader.load(directory)) {
            modules.put(module.getName(), module);
        }
        reporter.updateActiveModules(activeModuleCount());
    }

    /**
     * Loads modules that other plugins bundle inside their own jars ({@code accounts-module.yml} /
     * {@code accounts-modules/*.yml}) by scanning the server's {@code pluginsDir}. A bundled module
     * never replaces one already registered, so a server-local file loaded via
     * {@link #loadModules(Path)} wins over a plugin's bundled default whatever the call order.
     */
    public void loadPluginJarModules(Path pluginsDir) {
        for (Module module : new PluginJarModuleLoader().load(pluginsDir)) {
            modules.putIfAbsent(module.getName(), module);
        }
        reporter.updateActiveModules(activeModuleCount());
    }

    /** Loads community-contributed compiled modules from jars in {@code jarDirectory}. */
    public void loadJarModules(Path jarDirectory) {
        for (Module module : new JarModuleLoader().load(jarDirectory)) {
            modules.put(module.getName(), module);
        }
        reporter.updateActiveModules(activeModuleCount());
    }

    public Collection<Module> getModules() {
        return modules.values();
    }

    public void unloadModules() {
        modules.clear();
    }

    private int activeModuleCount() {
        return (int) modules.values().stream().filter(Module::isEnabled).count();
    }
}
