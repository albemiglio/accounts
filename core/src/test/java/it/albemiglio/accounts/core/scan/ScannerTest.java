package it.albemiglio.accounts.core.scan;

import it.albemiglio.accounts.core.database.DB;
import it.albemiglio.accounts.core.database.SQLite;
import it.albemiglio.accounts.core.modules.replacers.UuidCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScannerTest {

    private static final UUID PROBE = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID DECOY = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");

    @Test
    void findsUuidNamedFilesAndDirectoriesButSkipsJars(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path userdata = Files.createDirectories(plugins.resolve("pluginA/userdata"));
        Files.writeString(userdata.resolve(PROBE + ".yml"), "money: 5\n");
        Path history = Files.createDirectories(plugins.resolve("pluginA/history"));
        Files.createDirectories(history.resolve(PROBE.toString()));
        Files.write(plugins.resolve("Fake.jar"), ("junk " + PROBE).getBytes(StandardCharsets.UTF_8));

        List<ScanFinding> findings = new Scanner().scan(plugins, PROBE);

        assertEquals(2, findings.size());
        ScanFinding file = onlyWith(findings, "yml");
        assertEquals("pluginA", file.getPluginName());
        assertEquals(ScanFinding.Kind.FILE, file.getKind());
        assertEquals(userdata, file.getPath());
        ScanFinding bare = onlyWith(findings, "");
        assertEquals(ScanFinding.Kind.FILE, bare.getKind());
        assertEquals(history, bare.getPath());
    }

    @Test
    void findsTheProbeInsideTextFilesOnlyWhereItAppears(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path pluginB = Files.createDirectories(plugins.resolve("pluginB"));
        Files.writeString(pluginB.resolve("data.yml"), PROBE + ": Notch\n");
        Files.writeString(pluginB.resolve("config.yml"), "owner: " + DECOY + "\n");

        List<ScanFinding> findings = new Scanner().scan(plugins, PROBE);

        assertEquals(1, findings.size());
        assertEquals(ScanFinding.Kind.CONTENT, findings.get(0).getKind());
        assertEquals("pluginB", findings.get(0).getPluginName());
        assertEquals(pluginB.resolve("data.yml"), findings.get(0).getPath());
    }

    @Test
    void findsSqliteColumnsInEveryStoredFormat(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path dbFile = Files.createDirectories(plugins.resolve("pluginC")).resolve("db.sqlite");
        seedDatabase(dbFile);

        List<ScanFinding> findings = new Scanner().scan(plugins, PROBE);

        assertEquals(3, findings.size());
        assertEquals(UuidCodec.DASHED, formatOf(findings, "uuid"));
        assertEquals(UuidCodec.UNDASHED, formatOf(findings, "mojang_id"));
        assertEquals(UuidCodec.BINARY, formatOf(findings, "raw"));
        for (ScanFinding finding : findings) {
            assertEquals(ScanFinding.Kind.SQLITE, finding.getKind());
            assertEquals("pluginC", finding.getPluginName());
            assertEquals(dbFile, finding.getPath());
            assertEquals("players", finding.getTable());
        }
    }

    @Test
    void reportsBinaryFilesContainingTheProbeBytes(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path pluginD = Files.createDirectories(plugins.resolve("pluginD"));
        byte[] ascii = PROBE.toString().getBytes(StandardCharsets.US_ASCII);
        byte[] blob = new byte[ascii.length + 4];
        blob[0] = (byte) 0x89;
        blob[1] = (byte) 0xFF;
        System.arraycopy(ascii, 0, blob, 2, ascii.length);
        blob[blob.length - 2] = (byte) 0xFE;
        Files.write(pluginD.resolve("cache.bin"), blob);

        List<ScanFinding> findings = new Scanner().scan(plugins, PROBE);

        assertEquals(1, findings.size());
        assertEquals(ScanFinding.Kind.UNKNOWN_BINARY, findings.get(0).getKind());
        assertEquals(pluginD.resolve("cache.bin"), findings.get(0).getPath());
    }

    @Test
    void ignoresTheProbeEmbeddedInLongerHex(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path pluginE = Files.createDirectories(plugins.resolve("pluginE"));
        Files.writeString(pluginE.resolve("data.yml"), "key: deadbeef" + PROBE + "cafe\n");

        List<ScanFinding> findings = new Scanner().scan(plugins, PROBE);

        assertTrue(findings.isEmpty(), findings.toString());
    }

    @Test
    void neverTouchesTheLiveDatabaseFile(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path pluginC = Files.createDirectories(plugins.resolve("pluginC"));
        Path dbFile = pluginC.resolve("db.sqlite");
        seedDatabase(dbFile);
        byte[] before = Files.readAllBytes(dbFile);
        FileTime mtime = Files.getLastModifiedTime(dbFile);
        List<String> siblings = names(pluginC);

        new Scanner().scan(plugins, PROBE);

        assertArrayEquals(before, Files.readAllBytes(dbFile));
        assertEquals(mtime, Files.getLastModifiedTime(dbFile));
        assertEquals(siblings, names(pluginC));
    }

    @Test
    void wrapsCorruptDatabasesIntoANoteInsteadOfAborting(@TempDir Path dir) throws Exception {
        Path plugins = Files.createDirectories(dir.resolve("plugins"));
        Path pluginF = Files.createDirectories(plugins.resolve("pluginF"));
        Files.writeString(pluginF.resolve("broken.db"), "this is not a database\n");
        Files.writeString(pluginF.resolve("data.yml"), PROBE + ": Notch\n");

        List<ScanFinding> findings = new Scanner().scan(plugins, PROBE);

        assertEquals(2, findings.size());
        ScanFinding error = findings.stream().filter(f -> f.getKind() == ScanFinding.Kind.SQLITE)
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(pluginF.resolve("broken.db"), error.getPath());
        assertNull(error.getTable());
        assertNotNull(error.getNote());
        assertTrue(findings.stream().anyMatch(f -> f.getKind() == ScanFinding.Kind.CONTENT));
    }

    private static void seedDatabase(Path dbFile) throws Exception {
        DB seed = new SQLite(null, 0, null, null, dbFile.toString());
        try (Connection c = seed.getConnection()) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE players (uuid TEXT, mojang_id TEXT, raw BLOB, decoy TEXT)");
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO players VALUES (?, ?, ?, ?)")) {
                ps.setString(1, PROBE.toString());
                ps.setString(2, PROBE.toString().replace("-", ""));
                ps.setBytes(3, ByteBuffer.allocate(16)
                        .putLong(PROBE.getMostSignificantBits())
                        .putLong(PROBE.getLeastSignificantBits())
                        .array());
                ps.setString(4, DECOY.toString());
                ps.executeUpdate();
            }
        }
        seed.close();
    }

    private static ScanFinding onlyWith(List<ScanFinding> findings, String extension) {
        List<ScanFinding> matches = findings.stream()
                .filter(f -> extension.equals(f.getExtension())).collect(Collectors.toList());
        assertEquals(1, matches.size(), findings.toString());
        return matches.get(0);
    }

    private static UuidCodec formatOf(List<ScanFinding> findings, String column) {
        return findings.stream().filter(f -> column.equals(f.getColumn()))
                .findFirst().orElseThrow(AssertionError::new).getFormat();
    }

    private static List<String> names(Path directory) throws Exception {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }
}
