package it.albemiglio.accounts.core.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Rewrites every reference to a player's old UUID to the new one, anywhere in an NBT tree. Minecraft
 * has stored player UUIDs three ways over its history, and a world from any version can hold any of
 * them, so all three are matched:
 * <ul>
 *   <li>a 4-int array — the form since 1.16 (Owner, SkullOwner.Id, CustomBossEvents.Players, ...);</li>
 *   <li>a dashed string — the form before 1.16 (OwnerUUID, a string SkullOwner.Id, plugin tags);</li>
 *   <li>a {@code <name>Most}/{@code <name>Least} long pair — pre-1.16 projectile/owner references.</li>
 * </ul>
 * Scan-all rather than a fixed tag map: it catches vanilla, modded and datapack tags from 1.8 onward,
 * and a 128-bit value coinciding with the UUID by accident is impossible, so there are no false hits.
 * This is how "nothing is lost" stays true across every version.
 */
public final class UuidNbtRewriter {

    private static final String MOST = "Most";
    private static final String LEAST = "Least";

    private final int[] oldArr;
    private final int[] newArr;
    private final String oldDashed;
    private final String newDashed;
    private final long oldMost;
    private final long oldLeast;
    private final long newMost;
    private final long newLeast;

    public UuidNbtRewriter(UUID oldId, UUID newId) {
        this.oldArr = toIntArray(oldId);
        this.newArr = toIntArray(newId);
        this.oldDashed = oldId.toString();
        this.newDashed = newId.toString();
        this.oldMost = oldId.getMostSignificantBits();
        this.oldLeast = oldId.getLeastSignificantBits();
        this.newMost = newId.getMostSignificantBits();
        this.newLeast = newId.getLeastSignificantBits();
    }

    /** Rewrites in place; returns how many references were changed. */
    public int rewrite(Tag<?> tag) {
        if (tag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) tag;
            int count = rewriteLongPairs(compound);
            for (String key : new ArrayList<>(compound.keySet())) {
                Tag<?> value = compound.get(key);
                if (isOldIntArray(value)) {
                    compound.putIntArray(key, newArr.clone());
                    count++;
                } else if (isOldString(value)) {
                    compound.putString(key, newDashed);
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
                if (isOldIntArray(element)) {
                    @SuppressWarnings("unchecked")
                    ListTag<IntArrayTag> intArrayList = (ListTag<IntArrayTag>) list;
                    intArrayList.set(i, new IntArrayTag(newArr.clone()));
                    count++;
                } else if (isOldString(element)) {
                    @SuppressWarnings("unchecked")
                    ListTag<StringTag> stringList = (ListTag<StringTag>) list;
                    stringList.set(i, new StringTag(newDashed));
                    count++;
                } else {
                    count += rewrite(element);
                }
            }
            return count;
        }
        return 0;
    }

    /** Rewrites any {@code <name>Most}/{@code <name>Least} long pair that together encode the old UUID. */
    private int rewriteLongPairs(CompoundTag compound) {
        int count = 0;
        for (String key : new ArrayList<>(compound.keySet())) {
            if (!key.endsWith(MOST)) {
                continue;
            }
            String leastKey = key.substring(0, key.length() - MOST.length()) + LEAST;
            Tag<?> most = compound.get(key);
            Tag<?> least = compound.get(leastKey);
            if (most instanceof LongTag && least instanceof LongTag
                    && ((LongTag) most).asLong() == oldMost
                    && ((LongTag) least).asLong() == oldLeast) {
                compound.putLong(key, newMost);
                compound.putLong(leastKey, newLeast);
                count++;
            }
        }
        return count;
    }

    private boolean isOldIntArray(Tag<?> tag) {
        return tag instanceof IntArrayTag && Arrays.equals(((IntArrayTag) tag).getValue(), oldArr);
    }

    private boolean isOldString(Tag<?> tag) {
        return tag instanceof StringTag && oldDashed.equalsIgnoreCase(((StringTag) tag).getValue());
    }

    public static int[] toIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
    }
}
