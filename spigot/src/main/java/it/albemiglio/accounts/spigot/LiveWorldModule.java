package it.albemiglio.accounts.spigot;

import it.albemiglio.accounts.core.modules.MigrationException;
import it.albemiglio.accounts.core.modules.Module;
import it.albemiglio.accounts.core.objects.Pair;
import it.albemiglio.accounts.core.objects.enums.Platform;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * The loaded-data half of the vanilla world migration. {@code NbtModule} rewrites the world files, but
 * only data at rest is safe to touch on disk: a chunk or a player the server holds in memory would be
 * overwritten by its next save, losing the rewrite. So for whatever the server currently has loaded —
 * a tamed pet near another player, a placed player-head, a head in an online player's inventory — this
 * rewrites the live objects through the Bukkit API instead, so the server persists the migrated value.
 *
 * <p>The Bukkit API is single-threaded, so the work is marshalled onto the main server thread; a
 * broadcast handled on the Redis thread waits for it, an admin's console migrate runs it inline.
 */
public final class LiveWorldModule extends Module {

    private final Plugin plugin;

    public LiveWorldModule(Plugin plugin) {
        super("live-world", Platform.SPIGOT);
        this.plugin = plugin;
    }

    @Override
    public void execute(Pair<UUID, UUID> migration) {
        UUID oldId = migration.getLeft();
        UUID newId = migration.getRight();
        if (Bukkit.isPrimaryThread()) {
            rewriteLoaded(oldId, newId);
            return;
        }
        try {
            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                rewriteLoaded(oldId, newId);
                return null;
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MigrationException("Interrupted migrating loaded world data", e);
        } catch (ExecutionException e) {
            throw new MigrationException("Failed migrating loaded world data", e.getCause());
        }
    }

    private void rewriteLoaded(UUID oldId, UUID newId) {
        OfflinePlayer newOwner = Bukkit.getOfflinePlayer(newId);
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Tameable) {
                    Tameable pet = (Tameable) entity;
                    AnimalTamer owner = pet.getOwner();
                    if (owner != null && oldId.equals(owner.getUniqueId())) {
                        pet.setOwner(newOwner);
                    }
                }
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Skull) {
                        Skull skull = (Skull) state;
                        OfflinePlayer head = skull.getOwningPlayer();
                        if (head != null && oldId.equals(head.getUniqueId())) {
                            skull.setOwningPlayer(newOwner);
                            skull.update(true, false);
                        }
                    }
                }
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            rewriteHeads(player.getInventory(), oldId, newOwner);
            rewriteHeads(player.getEnderChest(), oldId, newOwner);
        }
    }

    private void rewriteHeads(Inventory inventory, UUID oldId, OfflinePlayer newOwner) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof SkullMeta)) {
                continue;
            }
            SkullMeta skullMeta = (SkullMeta) meta;
            OfflinePlayer head = skullMeta.getOwningPlayer();
            if (head != null && oldId.equals(head.getUniqueId())) {
                skullMeta.setOwningPlayer(newOwner);
                item.setItemMeta(skullMeta);
                inventory.setItem(slot, item);
            }
        }
    }
}
