package it.albemiglio.accounts.core.scan;

import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.modules.replacers.UuidCodec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hunts for a probe UUID — a real player known to have data — across every plugin data folder under a
 * server's plugins directory. This is how a plugin with no template and no source gets a module: instead
 * of reverse-engineering its storage, the operator points the scanner at data the player is known to be
 * in, and it reports every place the uuid shows up — as a file/directory name, inside text files (same
 * whole-token rule as {@link it.albemiglio.accounts.core.modules.ContentModule}), in any column of any
 * SQLite file (all three {@link UuidCodec} encodings), or as raw bytes in an unknown binary file.
 * The scanner is strictly read-only on plugin data: SQLite files are probed on a temp-directory COPY
 * (the owning plugin may hold the live file) and nothing under the scanned root is ever written.
 * Findings feed {@link DraftWriter}, which turns them into disabled drafts for an operator to review.
 */
public class Scanner {

    /** Content probes stop at 5 MB: a uuid-keyed data file bigger than that is not a data file. */
    private static final long MAX_PROBE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "yml", "yaml", "json", "txt", "conf", "properties", "csv"));

    public List<ScanFinding> scan(Path pluginsDir, UUID probe) throws IOException {
        List<ScanFinding> findings = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("accounts-scan");
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(pluginsDir)) {
            List<Path> pluginDirs = new ArrayList<>();
            for (Path entry : entries) {
                // Only plugin DATA folders are scanned; the jars (and any stray top-level file) are not data.
                if (Files.isDirectory(entry)) {
                    pluginDirs.add(entry);
                }
            }
            Collections.sort(pluginDirs);
            for (Path pluginDir : pluginDirs) {
                scanPlugin(pluginDir, probe, tempDir, findings);
            }
        } finally {
            deleteRecursively(tempDir);
        }
        return findings;
    }

    private void scanPlugin(Path pluginDir, UUID probe, Path tempDir, List<ScanFinding> findings)
            throws IOException {
        String plugin = pluginDir.getFileName().toString();
        String dashed = probe.toString();
        // Same whole-token guard as ContentModule: a neighbouring hex digit or dash means the match is
        // a fragment of some longer id, never the player's UUID.
        Pattern token = Pattern.compile(
                "(?<![0-9a-fA-F-])" + Pattern.quote(dashed) + "(?![0-9a-fA-F-])");
        List<Path> entries;
        try (Stream<Path> walk = Files.walk(pluginDir)) {
            entries = walk.sorted().collect(Collectors.toList());
        }
        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            boolean namedByProbe = name.equals(dashed) || name.startsWith(dashed + ".");
            if (namedByProbe) {
                // The name is the key (EssentialsX-style); the module parameters are the parent
                // directory plus the extension — empty for a bare file or a whole per-player directory.
                String extension = name.equals(dashed) ? "" : name.substring(dashed.length() + 1);
                findings.add(ScanFinding.file(plugin, entry.getParent(), extension,
                        "found " + (Files.isDirectory(entry) ? "directory " : "file ") + name));
            }
            if (!Files.isRegularFile(entry)) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".db") || lower.endsWith(".sqlite")) {
                scanSqlite(plugin, entry, probe, tempDir, findings);
                continue;
            }
            if (Files.size(entry) > MAX_PROBE_BYTES) {
                continue;
            }
            byte[] bytes = Files.readAllBytes(entry);
            String content = decodeUtf8(bytes);
            if (content == null) {
                // Binary file. Latin-1 maps bytes 1:1 to chars, so contains() is a cheap byte search
                // for the dashed uuid's ASCII bytes. Evidence only — no draft can rewrite it.
                if (!namedByProbe && new String(bytes, StandardCharsets.ISO_8859_1).contains(dashed)) {
                    findings.add(ScanFinding.unknownBinary(plugin, entry,
                            "binary file contains the dashed uuid as raw bytes; format unknown"));
                }
            } else if (TEXT_EXTENSIONS.contains(extensionOf(lower)) && token.matcher(content).find()) {
                findings.add(ScanFinding.content(plugin, entry, "dashed uuid appears in the file content"));
            }
        }
    }

    private void scanSqlite(String plugin, Path dbFile, UUID probe, Path tempDir, List<ScanFinding> findings) {
        // NEVER open the live file — the owning plugin may hold it. Probe a copy; any sidecar files
        // the driver creates next to the copy die with the temp directory.
        SQLite db = null;
        try {
            Path copy = Files.createTempFile(tempDir, dbFile.getFileName().toString(), ".probe");
            Files.copy(dbFile, copy, StandardCopyOption.REPLACE_EXISTING);
            db = new SQLite(null, 0, null, null, copy.toString());
            try (Connection connection = db.getConnection()) {
                for (String table : tables(connection)) {
                    for (String column : columns(connection, table)) {
                        UuidCodec format = probeColumn(connection, table, column, probe);
                        if (format != null) {
                            findings.add(ScanFinding.sqlite(plugin, dbFile, table, column, format,
                                    "probe row found in " + table + "." + column));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // A corrupt file, a torn copy of a hot database, a .db that isn't sqlite at all — all
            // report-and-continue: one bad store must not abort the scan of a whole server.
            findings.add(ScanFinding.sqliteError(plugin, dbFile, "could not probe: " + e.getMessage()));
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private List<String> tables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private List<String> columns(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + quote(table) + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        return columns;
    }

    /**
     * SQLite typing is dynamic — a column declared TEXT can hold anything — so instead of trusting
     * declared types every column is probed with all three encodings. Equality (not substring) is the
     * right test: {@link it.albemiglio.accounts.core.modules.replacers.ColumnReplacer} can only rewrite
     * whole-value matches anyway.
     */
    private UuidCodec probeColumn(Connection connection, String table, String column, UUID probe)
            throws SQLException {
        for (UuidCodec codec : UuidCodec.values()) {
            String sql = "SELECT 1 FROM " + quote(table) + " WHERE " + quote(column) + " = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                codec.bind(ps, 1, probe);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return codec;
                    }
                }
            }
        }
        return null;
    }

    /** Table and column names come out of the scanned file itself, so they are data: always quoted. */
    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /** Strict decode, same as ContentModule: the lenient String constructor would call a binary file text. */
    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        List<Path> entries;
        try (Stream<Path> walk = Files.walk(dir)) {
            entries = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
        for (Path entry : entries) {
            Files.deleteIfExists(entry);
        }
    }
}
