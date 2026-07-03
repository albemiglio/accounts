package it.albemiglio.accounts.core.modules;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Discovers modules that third-party plugins ship inside their own jars, plugin.yml-style. A plugin
 * declares its migration by bundling {@code accounts-module.yml} (one module at the jar root) and/or
 * {@code accounts-modules/*.yml} (several, for the rename+content pairs) as plain resources — the exact
 * schema of the templates, no code and no dependency on accounts. On startup the engine scans the
 * server's plugins directory and picks them up automatically.
 *
 * <p>One faulty plugin must never break startup: a corrupt jar or an invalid descriptor is logged as a
 * warning (naming jar and entry) and skipped. Jars are visited in file-name order, and when two jars
 * declare the same module name the first wins. Server-local module files are the operator's override
 * and should win over anything bundled here — {@code ModuleService#loadPluginJarModules(Path)} composes
 * them that way.
 */
public final class PluginJarModuleLoader {

    private static final Logger LOG = Logger.getLogger(PluginJarModuleLoader.class.getName());
    private static final String ROOT_DESCRIPTOR = "accounts-module.yml";
    private static final String DESCRIPTOR_DIR = "accounts-modules/";

    private final YamlModuleFactory factory = new YamlModuleFactory();

    public List<Module> load(Path pluginsDir) {
        Map<String, Module> byName = new LinkedHashMap<>();
        if (!Files.isDirectory(pluginsDir)) {
            return new ArrayList<>(byName.values());
        }
        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : files) {
                if (Files.isRegularFile(jar)) {
                    jars.add(jar);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jars.sort(Comparator.comparing(jar -> jar.getFileName().toString()));
        for (Path jar : jars) {
            loadJar(jar, byName);
        }
        return new ArrayList<>(byName.values());
    }

    private void loadJar(Path jar, Map<String, Module> byName) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (isDescriptor(entry)) {
                    loadDescriptor(zip, entry, jar, byName);
                }
            }
        } catch (IOException e) {
            LOG.warning("Skipping unreadable plugin jar " + jar.getFileName() + ": " + e.getMessage());
        }
    }

    private static boolean isDescriptor(ZipEntry entry) {
        String name = entry.getName();
        if (name.equals(ROOT_DESCRIPTOR)) {
            return true;
        }
        if (entry.isDirectory() || !name.startsWith(DESCRIPTOR_DIR)) {
            return false;
        }
        String file = name.substring(DESCRIPTOR_DIR.length());
        return !file.contains("/") && (file.endsWith(".yml") || file.endsWith(".yaml"));
    }

    private void loadDescriptor(ZipFile zip, ZipEntry entry, Path jar, Map<String, Module> byName) {
        try (InputStream in = zip.getInputStream(entry)) {
            Module module = factory.build(new Yaml().load(in));
            Module first = byName.putIfAbsent(module.getName(), module);
            if (first != null) {
                LOG.warning("Duplicate module name '" + module.getName() + "' in " + jar.getFileName()
                        + " (" + entry.getName() + "), keeping the one loaded first");
            }
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Skipping invalid module descriptor " + entry.getName()
                    + " in " + jar.getFileName(), e);
        }
    }
}
