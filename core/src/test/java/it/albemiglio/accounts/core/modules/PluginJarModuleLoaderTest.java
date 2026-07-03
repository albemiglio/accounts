package it.albemiglio.accounts.core.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginJarModuleLoaderTest {

    private static String moduleYaml(String name, boolean enabled) {
        return String.join("\n",
                "name: " + name,
                "platform: SPIGOT",
                "type: file",
                "directory: plugins/" + name + "/userdata",
                "extension: yml",
                "enabled: " + enabled);
    }

    /** Writes a jar at {@code file} from alternating entry-name/entry-content pairs. */
    private static void jar(Path file, String... entries) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            for (int i = 0; i < entries.length; i += 2) {
                out.putNextEntry(new ZipEntry(entries[i]));
                out.write(entries[i + 1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    private static List<String> warningsDuring(Runnable action) {
        Logger log = Logger.getLogger(PluginJarModuleLoader.class.getName());
        List<String> warnings = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record.getMessage());
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        log.addHandler(handler);
        try {
            action.run();
        } finally {
            log.removeHandler(handler);
        }
        return warnings;
    }

    @Test
    void loadsSingleDescriptorAtTheJarRoot(@TempDir Path dir) throws Exception {
        jar(dir.resolve("auraskills.jar"),
                "plugin.yml", "name: AuraSkills",
                "accounts-module.yml", moduleYaml("auraskills-userdata", true));

        List<Module> modules = new PluginJarModuleLoader().load(dir);

        assertEquals(1, modules.size());
        assertEquals("auraskills-userdata", modules.get(0).getName());
        assertTrue(modules.get(0) instanceof FileModule);
        assertTrue(modules.get(0).isEnabled());
    }

    @Test
    void loadsEveryDescriptorUnderTheModulesDirectory(@TempDir Path dir) throws Exception {
        jar(dir.resolve("auraskills.jar"),
                "accounts-modules/rename.yml", moduleYaml("auraskills-rename", true),
                "accounts-modules/content.yaml", moduleYaml("auraskills-content", true),
                "accounts-modules/nested/deep.yml", moduleYaml("too-deep", true));

        List<Module> modules = new PluginJarModuleLoader().load(dir);

        assertEquals(2, modules.size());
        assertTrue(modules.stream().noneMatch(m -> "too-deep".equals(m.getName())));
    }

    @Test
    void loadsBothTheRootDescriptorAndTheModulesDirectory(@TempDir Path dir) throws Exception {
        jar(dir.resolve("kitchen-sink.jar"),
                "accounts-module.yml", moduleYaml("sink-main", true),
                "accounts-modules/a.yml", moduleYaml("sink-a", true),
                "accounts-modules/b.yml", moduleYaml("sink-b", true));

        assertEquals(3, new PluginJarModuleLoader().load(dir).size());
    }

    @Test
    void jarWithoutDescriptorsContributesNothing(@TempDir Path dir) throws Exception {
        jar(dir.resolve("plain-plugin.jar"), "plugin.yml", "name: PlainPlugin");

        assertEquals(List.of(), new PluginJarModuleLoader().load(dir));
    }

    @Test
    void brokenJarsAndDescriptorsAreSkippedWithAWarning(@TempDir Path dir) throws Exception {
        jar(dir.resolve("broken-yaml.jar"), "accounts-module.yml", "name: [unclosed");
        Files.writeString(dir.resolve("corrupt.jar"), "not a zip archive");
        jar(dir.resolve("healthy.jar"), "accounts-module.yml", moduleYaml("healthy", true));

        List<Module> modules = new ArrayList<>();
        List<String> warnings = warningsDuring(() -> modules.addAll(new PluginJarModuleLoader().load(dir)));

        assertEquals(1, modules.size());
        assertEquals("healthy", modules.get(0).getName());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("broken-yaml.jar") && w.contains("accounts-module.yml")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("corrupt.jar")));
    }

    @Test
    void ignoresNonJarFilesAndSubdirectories(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("readme.txt"), "not a jar");
        Files.createDirectories(dir.resolve("weird.jar"));
        Path subdir = Files.createDirectories(dir.resolve("OldPlugins"));
        jar(subdir.resolve("inner.jar"), "accounts-module.yml", moduleYaml("inner", true));

        assertEquals(List.of(), new PluginJarModuleLoader().load(dir));
    }

    @Test
    void firstJarWinsOnDuplicateModuleNames(@TempDir Path dir) throws Exception {
        jar(dir.resolve("a.jar"), "accounts-module.yml", moduleYaml("shared", true));
        jar(dir.resolve("b.jar"), "accounts-module.yml", moduleYaml("shared", false));

        List<Module> modules = new ArrayList<>();
        List<String> warnings = warningsDuring(() -> modules.addAll(new PluginJarModuleLoader().load(dir)));

        assertEquals(1, modules.size());
        assertTrue(modules.get(0).isEnabled(), "expected the module from a.jar to win");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("shared") && w.contains("b.jar")));
    }

    @Test
    void disabledDescriptorLoadsANotEnabledModule(@TempDir Path dir) throws Exception {
        jar(dir.resolve("sleepy.jar"), "accounts-module.yml", moduleYaml("sleepy", false));

        List<Module> modules = new PluginJarModuleLoader().load(dir);

        assertEquals(1, modules.size());
        assertFalse(modules.get(0).isEnabled());
    }
}
