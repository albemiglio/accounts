package it.albemiglio.accounts.core.database;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.modules.YamlModule;
import it.albemiglio.accounts.core.modules.replacers.ColumnReplacer;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2 is the one backend where opening the file with the wrong driver generation can corrupt it, so the
 * format sniff / version-pin gate gets the heaviest coverage here. Everything runs offline: real stores
 * are created with the test-scoped H2 dependency, and the "downloaded" driver jar is materialized from
 * the test classpath instead of Maven Central (only the env-gated last test touches the network).
 */
class H2DatabaseTest {

    private static final UUID OLD = new UUID(0L, 1L);
    private static final UUID NEW = new UUID(0L, 2L);

    // Header lines exactly as written by real drivers (captured from files created by 1.4.197/2.1.214).
    private static final String FORMAT_1_HEADER =
            "H:2,blockSize:1000,created:19f25422f2d,format:1,fletcher:badf3a7e\n";
    private static final String FORMAT_2_HEADER =
            "H:2,block:3,blockSize:1000,chunk:2,clean:1,created:19f25423078,format:2,version:2,fletcher:b9f915e0\n";
    private static final String FORMAT_3_HEADER = FORMAT_2_HEADER.replace("format:2", "format:3");
    private static final String PAGESTORE_HEADER =
            "-- H2 0.5/B -- \n-- H2 0.5/B -- \n-- H2 0.5/B -- \n";

    // --- requested version -> accepted store formats ---

    @Test
    void legacy14AcceptsPageStoreAndMvStoreFormat1() {
        assertArrayEquals(new int[]{0, 1}, H2.expectedFormats("1.4.197"));
        assertArrayEquals(new int[]{0, 1}, H2.expectedFormats("1.4.200"));
    }

    @Test
    void h220And21ExpectFormat2() {
        assertArrayEquals(new int[]{2}, H2.expectedFormats("2.0.206"));
        assertArrayEquals(new int[]{2}, H2.expectedFormats("2.1.214"));
    }

    @Test
    void h222AndNewerExpectFormat3() {
        assertArrayEquals(new int[]{3}, H2.expectedFormats("2.2.224"));
        assertArrayEquals(new int[]{3}, H2.expectedFormats("2.3.232"));
        assertArrayEquals(new int[]{3}, H2.expectedFormats("2.4.240"));
    }

    @Test
    void versionsOutsideTheKnownFamiliesAreRefused() {
        assertThrows(MigrationException.class, () -> H2.expectedFormats("3.0.0"));
        assertThrows(MigrationException.class, () -> H2.expectedFormats("banana"));
    }

    // --- header sniffing ---

    @Test
    void readsTheStoreFormatARealH2WritesToDisk(@TempDir Path dir) throws Exception {
        String base = realStore(dir);
        assertEquals(3, H2.storedFormat(base)); // the test-scoped 2.4.240 writes format 3
    }

    @Test
    void readsMvStoreFormatsFromKnownHeaderLayouts(@TempDir Path dir) throws Exception {
        assertEquals(1, H2.storedFormat(mvStore(dir, "a", FORMAT_1_HEADER)));
        assertEquals(2, H2.storedFormat(mvStore(dir, "b", FORMAT_2_HEADER)));
        // "formatRead" must not shadow the write format: the key has to match exactly.
        assertEquals(2, H2.storedFormat(mvStore(dir, "c",
                FORMAT_2_HEADER.replace("format:2", "format:2,formatRead:1"))));
    }

    @Test
    void recognisesTheLegacyPageStoreHeader(@TempDir Path dir) throws Exception {
        String base = dir.resolve("legacy").toString();
        writePadded(Paths.get(base + ".h2.db"), PAGESTORE_HEADER);
        assertEquals(0, H2.storedFormat(base));
    }

    @Test
    void missingStoreFileIsRefusedWithThePath(@TempDir Path dir) {
        String base = dir.resolve("nope").toString();
        MigrationException e = assertThrows(MigrationException.class, () -> H2.storedFormat(base));
        assertTrue(e.getMessage().contains(base + ".mv.db"), e.getMessage());
    }

    @Test
    void pathGivenWithTheSuffixGetsToldToDropIt(@TempDir Path dir) {
        MigrationException e = assertThrows(MigrationException.class,
                () -> H2.storedFormat(dir.resolve("data.mv.db").toString()));
        assertTrue(e.getMessage().contains("WITHOUT the"), e.getMessage());
    }

    @Test
    void garbageMvDbFileIsRefused(@TempDir Path dir) throws Exception {
        String base = dir.resolve("junk").toString();
        Files.write(Paths.get(base + ".mv.db"), "hello".getBytes(StandardCharsets.US_ASCII));
        MigrationException e = assertThrows(MigrationException.class, () -> H2.storedFormat(base));
        assertTrue(e.getMessage().contains("format"), e.getMessage());
    }

    // --- the anti-corruption gate on connect: mismatches name the version to pin instead ---

    @Test
    void refusesAFormat3StoreWhenPinnedTo14(@TempDir Path dir) throws Exception {
        String base = realStore(dir);
        H2 db = new H2(null, 0, "sa", "", base, "1.4.197", dir.resolve("cache"));
        MigrationException e = assertThrows(MigrationException.class, db::getConnection);
        assertTrue(e.getMessage().contains("1.4.197"), e.getMessage());
        assertTrue(e.getMessage().contains("2.4.240"), e.getMessage());
    }

    @Test
    void refusesAFormat2StoreWhenPinnedTo24(@TempDir Path dir) throws Exception {
        String base = mvStore(dir, "data", FORMAT_2_HEADER);
        H2 db = new H2(null, 0, "sa", "", base, "2.4.240", dir.resolve("cache"));
        MigrationException e = assertThrows(MigrationException.class, db::getConnection);
        assertTrue(e.getMessage().contains("2.1.214"), e.getMessage());
    }

    @Test
    void refusesAPageStoreFileWhenPinnedTo21(@TempDir Path dir) throws Exception {
        String base = dir.resolve("legacy").toString();
        writePadded(Paths.get(base + ".h2.db"), PAGESTORE_HEADER);
        H2 db = new H2(null, 0, "sa", "", base, "2.1.214", dir.resolve("cache"));
        MigrationException e = assertThrows(MigrationException.class, db::getConnection);
        assertTrue(e.getMessage().contains("1.4"), e.getMessage());
    }

    // --- driver cache ---

    @Test
    void refusesATamperedDriverJar(@TempDir Path dir) throws Exception {
        String base = mvStore(dir, "data", FORMAT_3_HEADER);
        Path cache = Files.createDirectories(dir.resolve("h2-drivers"));
        Files.write(cache.resolve("h2-2.4.240.jar"), "not the real jar".getBytes(StandardCharsets.US_ASCII));

        H2 db = new H2(null, 0, "sa", "", base, "2.4.240", cache);
        MigrationException e = assertThrows(MigrationException.class, db::getConnection);
        assertTrue(e.getMessage().contains("sha256"), e.getMessage());
    }

    @Test
    void unpinnedVersionSkipsChecksumVerification(@TempDir Path dir) throws Exception {
        String base = realStore(dir);
        // 2.3.230 has no embedded pin; the classpath jar stands in for it (any 2.2+ opens format 3).
        H2 db = new H2(null, 0, "sa", "", base, "2.3.230", cacheWithClasspathJar(dir, "2.3.230"));
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM \"axvaults_data\"")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        } finally {
            db.close();
        }
    }

    // --- end to end through the module pipeline ---

    @Test
    void migratesARealH2StoreEndToEndThroughTheModulePipeline(@TempDir Path dir) throws Exception {
        String base = realStore(dir);

        H2 db = new H2(null, 0, "sa", "", base, "2.4.240", cacheWithClasspathJar(dir, "2.4.240"));
        try {
            try (Connection probe = db.getConnection()) {
                assertNotSame(Driver.class.getClassLoader(), probe.getClass().getClassLoader(),
                        "the engine must use the version-pinned driver from its isolated loader");
            }
            // Quoted identifiers straight from the axvaults template: they must pass through untouched.
            Module module = new YamlModule("axvaults", Platform.SPIGOT, db,
                    List.of(new ColumnReplacer("\"axvaults_data\"", "\"uuid\"")));
            module.execute(Pair.of(OLD, NEW));
        } finally {
            db.close();
        }

        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + base, "sa", "");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT \"uuid\" FROM \"axvaults_data\"")) {
            rs.next();
            assertEquals(NEW.toString(), rs.getString(1));
        }
    }

    // Off by default so the suite stays network-free; H2_DOWNLOAD_TEST=true exercises the real fetch.
    @Test
    @EnabledIfEnvironmentVariable(named = "H2_DOWNLOAD_TEST", matches = "true")
    void downloadsAndVerifiesTheDriverFromMavenCentral(@TempDir Path dir) throws Exception {
        String base = realStore(dir);
        Path cache = dir.resolve("h2-drivers");

        H2 db = new H2(null, 0, "sa", "", base, "2.4.240", cache);
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM \"axvaults_data\"")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        } finally {
            db.close();
        }
        assertTrue(Files.isRegularFile(cache.resolve("h2-2.4.240.jar")));
    }

    /** A real store written by the test-scoped driver, with AxVaults-style quoted lowercase identifiers. */
    private static String realStore(Path dir) throws Exception {
        String base = dir.resolve("axvaults-data").toString();
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:" + base, "sa", "");
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE \"axvaults_data\" (\"uuid\" VARCHAR(36))");
            st.execute("INSERT INTO \"axvaults_data\" VALUES ('" + OLD + "')");
        }
        return base;
    }

    private static String mvStore(Path dir, String name, String header) throws IOException {
        Path file = dir.resolve(name + ".mv.db");
        writePadded(file, header);
        return dir.resolve(name).toString();
    }

    /** First 4096-byte block like H2 lays it out: the ASCII header line, then padding. */
    private static void writePadded(Path file, String header) throws IOException {
        byte[] block = new byte[4096];
        byte[] ascii = header.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(ascii, 0, block, 0, ascii.length);
        Files.write(file, block);
    }

    /** The 2.4.240 jar the test JVM already runs with, dropped into the cache under the given version. */
    private static Path cacheWithClasspathJar(Path dir, String version) throws Exception {
        Path cache = Files.createDirectories(dir.resolve("h2-drivers"));
        Path onClasspath = Paths.get(Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Files.copy(onClasspath, cache.resolve("h2-" + version + ".jar"));
        return cache;
    }
}
