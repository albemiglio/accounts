package it.albemiglio.accounts.core.modules;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class YamlModuleLoader {

    private final YamlModuleFactory factory = new YamlModuleFactory();

    public List<Module> load(Path directory) {
        List<Module> modules = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return modules;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.{yml,yaml}")) {
            Yaml yaml = new Yaml();
            for (Path file : files) {
                try (InputStream in = Files.newInputStream(file)) {
                    modules.add(factory.build(yaml.load(in)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return modules;
    }
}
