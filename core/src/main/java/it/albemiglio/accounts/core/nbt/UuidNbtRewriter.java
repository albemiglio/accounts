package it.albemiglio.accounts.core.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Rewrites every reference to a player's old UUID to the new one, anywhere in an NBT tree, by matching
 * the 4-int UUID form Minecraft uses (Owner, SkullOwner, CustomBossEvents.Players, projectile owners,
 * leashes, ...). Scan-all rather than a fixed tag map: it catches vanilla, modded and datapack tags
 * alike, and a 128-bit value coinciding with the UUID by accident is impossible, so there are no false
 * hits. This is how "nothing is lost".
 */
public final class UuidNbtRewriter {

    private final int[] oldArr;
    private final int[] newArr;

    public UuidNbtRewriter(UUID oldId, UUID newId) {
        this.oldArr = toIntArray(oldId);
        this.newArr = toIntArray(newId);
    }

    /** Rewrites in place; returns how many references were changed. */
    public int rewrite(Tag<?> tag) {
        if (tag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) tag;
            int count = 0;
            for (String key : new ArrayList<>(compound.keySet())) {
                Tag<?> value = compound.get(key);
                if (matches(value)) {
                    compound.putIntArray(key, newArr.clone());
                    count++;
                } else {
                    count += rewrite(value);
                }
            }
            return count;
        }
        if (tag instanceof ListTag) {
            ListTag<?> list = (ListTag<?>) tag;
            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                Tag<?> element = list.get(i);
                if (matches(element)) {
                    @SuppressWarnings("unchecked")
                    ListTag<IntArrayTag> intArrayList = (ListTag<IntArrayTag>) list;
                    intArrayList.set(i, new IntArrayTag(newArr.clone()));
                    count++;
                } else {
                    count += rewrite(element);
                }
            }
            return count;
        }
        return 0;
    }

    private boolean matches(Tag<?> tag) {
        return tag instanceof IntArrayTag && Arrays.equals(((IntArrayTag) tag).getValue(), oldArr);
    }

    public static int[] toIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
    }
}
