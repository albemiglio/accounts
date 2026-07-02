package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.io.AtomicFiles;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Migrates plugins that embed the player's dashed UUID inside text files (YAML, JSON, HOCON, plain
 * text) in shapes the other modules cannot reach: as a value, as a map key, as a list element, or
 * buried in a composite string like {@code "Steve:<uuid>%vote!time"}. The lowercase dashed form is a
 * 36-char token that cannot occur by accident, so migration is a whole-token text swap; parsing is
 * deliberately avoided so comments, formatting and key order survive byte-for-byte. Files that are
 * not valid UTF-8 are skipped rather than corrupted, and absent targets are ignored — a template may
 * point at a plugin that simply isn't installed on this backend.
 */
public class ContentModule extends Module {

    /** One place to look: a single file, or a directory scanned by file-name glob (optionally recursive). */
    public static final class Target {

        private final Path path;
        private final String pattern;
        private final boolean recursive;

        public Target(Path path, String pattern, boolean recursive) {
            this.path = path;
            this.pattern = pattern;
            this.recursive = recursive;
        }
    }

    private final List<Target> targets;

    public ContentModule(String name, Platform platform, List<Target> targets) {
        super(name, platform);
        this.targets = targets;
    }

    @Override
    public void execute(Pair<UUID, UUID> migration) {
        // Whole-token guard: a neighbouring hex digit or dash means the match is a fragment of some
        // longer id, never the player's UUID. Everything else (quotes, colons, brackets, '%', line
        // ends) is exactly what delimits keys, list items and composite strings.
        Pattern token = Pattern.compile(
                "(?<![0-9a-fA-F-])" + Pattern.quote(migration.getLeft().toString()) + "(?![0-9a-fA-F-])");
        String replacement = migration.getRight().toString();
        for (Target target : targets) {
            if (Files.isRegularFile(target.path)) {
                rewrite(target.path, token, replacement);
            } else if (Files.isDirectory(target.path)) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + target.pattern);
                try (Stream<Path> files = Files.find(target.path, target.recursive ? Integer.MAX_VALUE : 1,
                        (file, attrs) -> attrs.isRegularFile() && matcher.matches(file.getFileName()))) {
                    files.forEach(file -> rewrite(file, token, replacement));
                } catch (IOException | UncheckedIOException e) {
                    throw new MigrationException("Content migration failed for module " + getName(), e);
                }
            }
        }
    }

    private void rewrite(Path file, Pattern token, String replacement) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            String content;
            try {
                // Strict decode: the lenient String constructor would silently mangle a binary file
                // with replacement chars, and the mangled bytes would then be written back.
                content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException e) {
                return;
            }
            String replaced = token.matcher(content).replaceAll(replacement);
            if (!replaced.equals(content)) {
                byte[] out = replaced.getBytes(StandardCharsets.UTF_8);
                AtomicFiles.write(file, tmp -> Files.write(tmp, out));
            }
        } catch (IOException e) {
            throw new MigrationException("Content migration failed for " + file, e);
        }
    }
}
