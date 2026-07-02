package it.albemiglio.accounts.core.modules;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModuleTest {

    private static final UUID OLD = UUID.fromString("069a79f4-44e9-4726-a5be-fc65c822c0a9");
    private static final UUID NEW = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
    private static final UUID OTHER = UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6");

    private static ContentModule module(ContentModule.Target... targets) {
        return new ContentModule("worldguard-regions", Platform.SPIGOT, List.of(targets));
    }

    @Test
    void rewritesAYamlValueLeavingEveryOtherByteUntouched(@TempDir Path dir) throws IOException {
        String before = "# WorldGuard regions\nregions:\n  spawn:\n    owners:\n"
                + "      OwnerUUID: " + OLD + "\n    priority: 10\n";
        Files.writeString(dir.resolve("regions.yml"), before);

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertEquals(before.replace(OLD.toString(), NEW.toString()),
                Files.readString(dir.resolve("regions.yml")));
    }

    @Test
    void rewritesAUuidUsedAsAMapKey(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("players.yml"), OLD + ": Steve\n" + OTHER + ": Alex\n");

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertEquals(NEW + ": Steve\n" + OTHER + ": Alex\n",
                Files.readString(dir.resolve("players.yml")));
    }

    @Test
    void rewritesOnlyTheMatchingListElement(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("region.yml"),
                "unique-ids: [\"" + OLD + "\", \"" + OTHER + "\"]\n");

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertEquals("unique-ids: [\"" + NEW + "\", \"" + OTHER + "\"]\n",
                Files.readString(dir.resolve("region.yml")));
    }

    @Test
    void rewritesTheUuidInsideACompositeString(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("votes.yml"), "top-voter: \"Steve:" + OLD + "%1!1719855600\"\n");

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertEquals("top-voter: \"Steve:" + NEW + "%1!1719855600\"\n",
                Files.readString(dir.resolve("votes.yml")));
    }

    @Test
    void neverTouchesAUuidThatIsAFragmentOfALongerToken(@TempDir Path dir) throws IOException {
        String before = "a: f" + OLD + "\n"      // hex digit before
                + "b: " + OLD + "f\n"            // hex digit after
                + "c: 0-" + OLD + "\n"           // dash before
                + "d: " + OLD + "-0\n";          // dash after
        Files.writeString(dir.resolve("hashes.yml"), before);

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertEquals(before, Files.readString(dir.resolve("hashes.yml")));
    }

    @Test
    void replacesEveryOccurrenceInTheSameFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("data.yml"), "owner: " + OLD + "\n"
                + OLD + ": Steve\n"
                + "last-vote: \"Steve:" + OLD + "%1!123\"\n");

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        String after = Files.readString(dir.resolve("data.yml"));
        assertFalse(after.contains(OLD.toString()));
        assertEquals(3, count(after, NEW.toString()));
    }

    @Test
    void leavesANonMatchingFileCompletelyAlone(@TempDir Path dir) throws IOException {
        String before = "OwnerUUID: " + OTHER + "\n";
        Files.writeString(dir.resolve("regions.yml"), before);

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertEquals(before, Files.readString(dir.resolve("regions.yml")));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count()); // no temp files left behind
        }
    }

    @Test
    void recursiveTargetFindsNestedFiles(@TempDir Path dir) throws IOException {
        Path nested = dir.resolve("worlds").resolve("world_nether");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("regions.yml"), "OwnerUUID: " + OLD + "\n");

        module(new ContentModule.Target(dir, "regions.yml", true)).execute(Pair.of(OLD, NEW));

        assertEquals("OwnerUUID: " + NEW + "\n", Files.readString(nested.resolve("regions.yml")));
    }

    @Test
    void nonRecursiveTargetStaysAtTheTopLevel(@TempDir Path dir) throws IOException {
        Path nested = dir.resolve("worlds");
        Files.createDirectories(nested);
        Files.writeString(dir.resolve("regions.yml"), "OwnerUUID: " + OLD + "\n");
        Files.writeString(nested.resolve("regions.yml"), "OwnerUUID: " + OLD + "\n");

        module(new ContentModule.Target(dir, "regions.yml", false)).execute(Pair.of(OLD, NEW));

        assertEquals("OwnerUUID: " + NEW + "\n", Files.readString(dir.resolve("regions.yml")));
        assertEquals("OwnerUUID: " + OLD + "\n", Files.readString(nested.resolve("regions.yml")));
    }

    @Test
    void patternFiltersByFileName(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("regions.yml"), "OwnerUUID: " + OLD + "\n");
        Files.writeString(dir.resolve("config.yml"), "OwnerUUID: " + OLD + "\n");

        module(new ContentModule.Target(dir, "regions.yml", false)).execute(Pair.of(OLD, NEW));

        assertEquals("OwnerUUID: " + NEW + "\n", Files.readString(dir.resolve("regions.yml")));
        assertEquals("OwnerUUID: " + OLD + "\n", Files.readString(dir.resolve("config.yml")));
    }

    @Test
    void aTargetMayPointStraightAtAFile(@TempDir Path dir) throws IOException {
        Path rent = dir.resolve("rent.yml");
        Files.writeString(rent, "renter: " + OLD + "\n");

        // pattern and recursive are meaningless for a file target and must be ignored
        module(new ContentModule.Target(rent, "does-not-match", true)).execute(Pair.of(OLD, NEW));

        assertEquals("renter: " + NEW + "\n", Files.readString(rent));
    }

    @Test
    void aMissingDirectoryIsSkippedSilently(@TempDir Path dir) {
        Path absent = dir.resolve("plugins").resolve("NotInstalled");

        module(new ContentModule.Target(absent, "*", true)).execute(Pair.of(OLD, NEW)); // must not throw

        assertFalse(Files.exists(absent));
    }

    @Test
    void aBinaryFileIsSkippedAndLeftByteIdentical(@TempDir Path dir) throws IOException {
        byte[] uuid = OLD.toString().getBytes(StandardCharsets.UTF_8);
        byte[] before = new byte[uuid.length + 4];
        before[0] = (byte) 0x89;
        before[1] = (byte) 0xC3; // invalid UTF-8 sequence: 0xC3 not followed by a continuation byte
        before[2] = 0x28;
        System.arraycopy(uuid, 0, before, 3, uuid.length);
        before[before.length - 1] = (byte) 0xFF;
        Files.write(dir.resolve("skin.png"), before);

        module(new ContentModule.Target(dir, "*", false)).execute(Pair.of(OLD, NEW));

        assertArrayEquals(before, Files.readAllBytes(dir.resolve("skin.png")));
    }

    @Test
    void appliesEveryConfiguredTarget(@TempDir Path dir) throws IOException {
        Path worldguard = dir.resolve("WorldGuard");
        Files.createDirectories(worldguard);
        Files.writeString(worldguard.resolve("regions.yml"), "OwnerUUID: " + OLD + "\n");
        Path rent = dir.resolve("rent.yml");
        Files.writeString(rent, "renter: " + OLD + "\n");

        module(new ContentModule.Target(worldguard, "*.yml", false),
                new ContentModule.Target(rent, "*", false)).execute(Pair.of(OLD, NEW));

        assertTrue(Files.readString(worldguard.resolve("regions.yml")).contains(NEW.toString()));
        assertTrue(Files.readString(rent).contains(NEW.toString()));
    }

    private static int count(String haystack, String needle) {
        int occurrences = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            occurrences++;
        }
        return occurrences;
    }
}
