package cc.spea.headstones;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeadStones extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.getPlayer().setRespawnLocation(null);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) return;

        Block skullBlock = getBlock(event);
        Skull skull = (Skull) skullBlock.getState();

        // Set spawn to there
        event.getEntity().setBedSpawnLocation(skullBlock.getLocation(), true);

        List<String> inventory = new ArrayList<>();
        for (ItemStack is : event.getDrops()) {
            inventory.add(itemStackToBase64(is));
            getLogger().info(event.getEntity().getName() + " item: " + is);
        }
        getLogger().info(event.getEntity().getName() + " xp: " + event.getDroppedExp());
        getLogger().info(event.getEntity().getName() + " death message: " + event.getDeathMessage());

        skull.getPersistentDataContainer().set(new NamespacedKey(this, "items"), PersistentDataType.LIST.strings(), inventory);
        skull.getPersistentDataContainer().set(new NamespacedKey(this, "xp"), PersistentDataType.INTEGER, event.getDroppedExp());
        skull.getPersistentDataContainer().set(new NamespacedKey(this, "deathMessage"), PersistentDataType.STRING, event.getDeathMessage().replace(event.getEntity().getName(), "Player"));

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(event.getEntity().getUniqueId()));
        skull.update();

        Bukkit.broadcastMessage(skull.getPersistentDataContainer().toString());
    }

    private static Block getBlock(PlayerDeathEvent event) {
        Location loc = event.getEntity().getLocation();
        Block skullBlock = loc.getBlock();

        // Go up to find a valid spot
        while (skullBlock.getType() != Material.AIR) {
            skullBlock = loc.add(0, 1, 0).getBlock();
        }
        // Go down to the surface
        while (skullBlock.getType() == Material.AIR) {
            skullBlock = loc.add(0, -1, 0).getBlock();
        }
        skullBlock = loc.add(0, 1, 0).getBlock();

        skullBlock.setType(Material.PLAYER_HEAD);
        return skullBlock;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.PLAYER_HEAD) breakPlayerHead(event.getPlayer(), event.getBlock());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.getToBlock().getType() == Material.PLAYER_HEAD) {
            if (((Skull) event.getToBlock().getState()).getPersistentDataContainer().has(new NamespacedKey(this, "items"))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getBlock().getType() == Material.PLAYER_HEAD) {
            if (((Skull) event.getBlock().getState()).getPersistentDataContainer().has(new NamespacedKey(this, "items"))) {
                event.setCancelled(true);
            }
        }
    }

    //Todo: Fix dispenser

    /**
     * The "BOAAY" fix
     */
    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent event) {
        if (event.getBlock().getType() == Material.PLAYER_HEAD) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || (event.getClickedBlock().getType() != Material.PLAYER_HEAD && event.getClickedBlock().getType() != Material.PLAYER_WALL_HEAD)) return;
        if (event.getItem() == null || event.getItem().getType() != Material.ENCHANTED_GOLDEN_APPLE) return;

        Skull playerHead = (Skull) event.getClickedBlock().getState();

        if (playerHead.getOwningPlayer().isOnline() && playerHead.getOwningPlayer().getPlayer().getGameMode() == GameMode.SPECTATOR) {
            Player player = playerHead.getOwningPlayer().getPlayer();
            Location blockLoc = event.getClickedBlock().getLocation().add(0.5, 0, 0.5);
            player.teleport(blockLoc);
            blockLoc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, blockLoc.clone().add(0, 1, 0), 350, 0, 0, 0, 2);
            blockLoc.getWorld().playSound(blockLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);

            ItemStack savedItem = event.getItem().clone();
            savedItem.setAmount(1);
            event.getItem().setAmount(event.getItem().getAmount() - 1);

            Bukkit.getScheduler().scheduleSyncDelayedTask(this , () -> {
                if (!player.isOnline()) {
                    blockLoc.getWorld().playSound(blockLoc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    event.getPlayer().getInventory().addItem(savedItem);
                    return;
                }

                blockLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, blockLoc.clone().add(0, 1, 0), 200, 0, 0, 0, 0.1);
                blockLoc.getWorld().playSound(blockLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);

                player.teleport(blockLoc);
                player.setGameMode(getServer().getDefaultGameMode());

                breakPlayerHead(event.getPlayer(), event.getClickedBlock());

                event.getClickedBlock().setType(Material.AIR);
                Bukkit.broadcastMessage(ChatColor.YELLOW + event.getPlayer().getName() + " brought " + player.getName() + " back!");
            }, 40L);
        }
    }

    public void breakPlayerHead(Player player, Block block) {
        Location loc = block.getLocation();
        Skull skull = (Skull) block.getState();

        String deathMessage = skull.getPersistentDataContainer().get(new NamespacedKey(this, "deathMessage"), PersistentDataType.STRING);
        if (deathMessage != null) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " discovered remains: " + deathMessage);
        } else {
            return;
        }

        try {
            List<String> items = skull.getPersistentDataContainer().get(new NamespacedKey(this, "items"), PersistentDataType.LIST.strings());
            if (items != null) {
                for (String it : items) {
                    try {
                        ItemStack itemStack = itemStackFromBase64(it);
                        loc.getWorld().dropItemNaturally(loc, itemStack);
                    } catch (IOException e) {
                    }
                }
            }
        } catch (Exception e) {}

        Integer xp = skull.getPersistentDataContainer().get(new NamespacedKey(this, "xp"), PersistentDataType.INTEGER);
        if (xp != null && xp != 0) {
            loc.getWorld().spawn(loc, ExperienceOrb.class).setExperience(xp);
        }

        for (Player p : getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        }

        loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc.add(0.5, 0.5, 0.5), 1000, 0.0, 0.0, 0.0, 0.05, Material.REDSTONE_BLOCK.createBlockData());
    }

    /**
     * Gets one {@link ItemStack} from Base64 string.
     * <a href="https://gist.github.com/graywolf336/8153678?permalink_comment_id=4536153#gistcomment-4536153">Source</a>
     * @param data Base64 string to convert to {@link ItemStack}.
     * @return {@link ItemStack} created from the Base64 string.
     * @throws IOException
     */
    public static ItemStack itemStackFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item;

            // Read the serialized inventory
            item = (ItemStack) dataInput.readObject();

            dataInput.close();
            return item;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    /**
     * A method to serialize one {@link ItemStack} to Base64 String.
     * <a href="https://gist.github.com/graywolf336/8153678?permalink_comment_id=4536153#gistcomment-4536153">Source/a>
     * @param item to turn into a Base64 String.
     * @return Base64 string of the item.
     * @throws IllegalStateException
     */
    public static String itemStackToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Save every element
            dataOutput.writeObject(item);

            // Serialize that array
            dataOutput.close();
            return new String(Base64Coder.encode(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
}
