package it.albemiglio.accounts.core.modules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads community-contributed modules from compiled jars in a directory, through the
 * {@link ModuleProvider} SPI. The jars are loaded in a child classloader of accounts' own, so they can
 * reference {@link Module}/{@link ModuleProvider} and ship their own dependencies.
 */
public final class JarModuleLoader {

    public List<Module> load(Path jarDirectory) {
        if (!Files.isDirectory(jarDirectory)) {
            return new ArrayList<>();
        }
        List<URL> urls = new ArrayList<>();
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(jarDirectory, "*.jar")) {
            for (Path jar : jars) {
                urls.add(jar.toUri().toURL());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (urls.isEmpty()) {
            return new ArrayList<>();
        }
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        return fromClassLoader(classLoader);
    }

    static List<Module> fromClassLoader(ClassLoader classLoader) {
        List<Module> modules = new ArrayList<>();
        for (ModuleProvider provider : ServiceLoader.load(ModuleProvider.class, classLoader)) {
            modules.addAll(provider.modules());
        }
        return modules;
    }
}
