package it.albemiglio.accounts.core.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.ListTag;
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
}
