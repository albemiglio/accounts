package it.albemiglio.accounts.core.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidNbtRewriterTest {

    private static final UUID OLD = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID NEW = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void rewritesEveryMatchingUuidNestedAnywhere() {
        int[] oldArr = UuidNbtRewriter.toIntArray(OLD);
        int[] newArr = UuidNbtRewriter.toIntArray(NEW);

        CompoundTag root = new CompoundTag();
        root.putIntArray("Owner", oldArr.clone());          // an entity owner
        CompoundTag nested = new CompoundTag();
        nested.putIntArray("SkullOwner", oldArr.clone());   // a block-entity owner
        nested.putInt("other", 5);                          // unrelated data
        root.put("nested", nested);
        ListTag<IntArrayTag> players = new ListTag<>(IntArrayTag.class);
        players.add(new IntArrayTag(oldArr.clone()));       // CustomBossEvents.Players style
        root.put("Players", players);

        int count = new UuidNbtRewriter(OLD, NEW).rewrite(root);

        assertEquals(3, count);
        assertArrayEquals(newArr, root.getIntArray("Owner"));
        assertArrayEquals(newArr, root.getCompoundTag("nested").getIntArray("SkullOwner"));
        assertArrayEquals(newArr, ((IntArrayTag) root.getListTag("Players").get(0)).getValue());
        assertEquals(5, root.getCompoundTag("nested").getInt("other"));
    }

    @Test
    void leavesNonMatchingArraysAlone() {
        int[] newArr = UuidNbtRewriter.toIntArray(NEW);
        CompoundTag root = new CompoundTag();
        root.putIntArray("SomeOtherUuid", newArr.clone());   // already the new id, or an unrelated uuid
        root.putIntArray("NotAUuid", new int[]{1, 2, 3});    // wrong length

        int count = new UuidNbtRewriter(OLD, NEW).rewrite(root);

        assertEquals(0, count);
        assertArrayEquals(newArr, root.getIntArray("SomeOtherUuid"));
    }

    @Test
    void rewritesStringFormUuidsUsedBeforeMc1_16() {
        CompoundTag root = new CompoundTag();
        root.putString("OwnerUUID", OLD.toString());            // pre-1.16 pet owner
        CompoundTag skull = new CompoundTag();
        skull.putString("Id", OLD.toString().toUpperCase());    // some writers used upper-case
        root.put("SkullOwner", skull);
        ListTag<StringTag> trusted = new ListTag<>(StringTag.class);
        trusted.add(new StringTag(OLD.toString()));             // a plugin's trusted-uuid list
        root.put("Trusted", trusted);

        int count = new UuidNbtRewriter(OLD, NEW).rewrite(root);

        assertEquals(3, count);
        assertEquals(NEW.toString(), root.getString("OwnerUUID"));
        assertEquals(NEW.toString(), root.getCompoundTag("SkullOwner").getString("Id"));
        assertEquals(NEW.toString(), ((StringTag) root.getListTag("Trusted").get(0)).getValue());
    }

    @Test
    void rewritesMostLeastLongPairUuidsUsedBeforeMc1_16() {
        CompoundTag root = new CompoundTag();
        root.putLong("OwnerUUIDMost", OLD.getMostSignificantBits());   // pre-1.16 projectile/pet owner
        root.putLong("OwnerUUIDLeast", OLD.getLeastSignificantBits());
        root.putLong("score", 42L);

        int count = new UuidNbtRewriter(OLD, NEW).rewrite(root);

        assertEquals(1, count);
        assertEquals(NEW.getMostSignificantBits(), root.getLong("OwnerUUIDMost"));
        assertEquals(NEW.getLeastSignificantBits(), root.getLong("OwnerUUIDLeast"));
        assertEquals(42L, root.getLong("score"));
    }

    @Test
    void leavesNonMatchingStringsAndLongPairsAlone() {
        CompoundTag root = new CompoundTag();
        root.putString("name", "Notch");                     // not a uuid
        root.putString("SomeOtherUuid", NEW.toString());     // already the new id
        root.putLong("ScoreMost", 1L);                       // a Most/Least pair that isn't the old uuid
        root.putLong("ScoreLeast", 2L);

        int count = new UuidNbtRewriter(OLD, NEW).rewrite(root);

        assertEquals(0, count);
        assertEquals("Notch", root.getString("name"));
        assertEquals(1L, root.getLong("ScoreMost"));
        assertEquals(2L, root.getLong("ScoreLeast"));
    }
}
