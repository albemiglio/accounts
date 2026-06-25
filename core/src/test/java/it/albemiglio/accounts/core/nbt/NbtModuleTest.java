package it.albemiglio.accounts.core.nbt;

import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import net.querz.mca.Chunk;
import net.querz.mca.LoadFlags;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.ListTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NbtModuleTest {

    private static final UUID OLD = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID NEW = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void rewritesAPetOwnerInARegionFile(@TempDir Path worldDir) throws IOException {
        writeRegionWithOwnedWolf(worldDir, OLD);

        new NbtModule("world", Platform.SPIGOT, worldDir).execute(Pair.of(OLD, NEW));

        assertArrayEquals(UuidNbtRewriter.toIntArray(NEW), readWolfOwner(worldDir));
    }

    @Test
    void rewritesABossBarViewerInLevelDat(@TempDir Path worldDir) throws IOException {
        writeLevelDatWithBossBarViewer(worldDir, OLD);

        new NbtModule("world", Platform.SPIGOT, worldDir).execute(Pair.of(OLD, NEW));

        assertArrayEquals(UuidNbtRewriter.toIntArray(NEW), readBossBarViewer(worldDir));
    }

    private static void writeLevelDatWithBossBarViewer(Path worldDir, UUID viewer) throws IOException {
        ListTag<IntArrayTag> players = new ListTag<>(IntArrayTag.class);
        players.add(new IntArrayTag(UuidNbtRewriter.toIntArray(viewer)));
        CompoundTag bar = new CompoundTag();
        bar.put("Players", players);
        CompoundTag bosses = new CompoundTag();
        bosses.put("minecraft:bar", bar);
        CompoundTag data = new CompoundTag();
        data.put("CustomBossEvents", bosses);
        CompoundTag root = new CompoundTag();
        root.put("Data", data);

        Files.createDirectories(worldDir);
        NBTUtil.write(new NamedTag("", root), worldDir.resolve("level.dat").toFile());
    }

    private static int[] readBossBarViewer(Path worldDir) throws IOException {
        NamedTag nt = NBTUtil.read(worldDir.resolve("level.dat").toFile());
        ListTag<?> players = ((CompoundTag) nt.getTag())
                .getCompoundTag("Data").getCompoundTag("CustomBossEvents")
                .getCompoundTag("minecraft:bar").getListTag("Players");
        return ((IntArrayTag) players.get(0)).getValue();
    }

    private static void writeRegionWithOwnedWolf(Path worldDir, UUID owner) throws IOException {
        CompoundTag wolf = new CompoundTag();
        wolf.putString("id", "minecraft:wolf");
        wolf.putIntArray("Owner", UuidNbtRewriter.toIntArray(owner));
        ListTag<CompoundTag> entities = new ListTag<>(CompoundTag.class);
        entities.add(wolf);
        CompoundTag level = new CompoundTag();
        level.putInt("xPos", 0);
        level.putInt("zPos", 0);
        level.putString("Status", "full");
        level.put("Entities", entities);
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 2975);
        root.put("Level", level);

        MCAFile mca = new MCAFile(0, 0);
        mca.setChunk(0, new Chunk(root));
        Path region = worldDir.resolve("region");
        Files.createDirectories(region);
        MCAUtil.write(mca, region.resolve("r.0.0.mca").toFile());
    }

    private static int[] readWolfOwner(Path worldDir) throws IOException {
        MCAFile mca = MCAUtil.read(worldDir.resolve("region").resolve("r.0.0.mca").toFile(), LoadFlags.RAW);
        CompoundTag handle = mca.getChunk(0).getHandle();
        ListTag<?> entities = handle.getCompoundTag("Level").getListTag("Entities");
        return ((CompoundTag) entities.get(0)).getIntArray("Owner");
    }
}
