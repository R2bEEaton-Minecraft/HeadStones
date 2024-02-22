package cc.spea.headstones;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeadStones extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) return;

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
        Skull skull = (Skull) skullBlock.getState();

        List<String> inventory = new ArrayList<>();
        for (ItemStack is : event.getDrops()) {
            inventory.add(itemStackToBase64(is));
        }

        skull.getPersistentDataContainer().set(new NamespacedKey(this, "items"), PersistentDataType.LIST.strings(), inventory);
        skull.getPersistentDataContainer().set(new NamespacedKey(this, "xp"), PersistentDataType.INTEGER, event.getDroppedExp());
        skull.getPersistentDataContainer().set(new NamespacedKey(this, "deathMessage"), PersistentDataType.STRING, event.getDeathMessage().replace(event.getEntity().getName(), "Player"));

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(event.getEntity().getUniqueId()));
        skull.update();
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

    public void breakPlayerHead(Player player, Block block) {
        Location loc = block.getLocation();
        Skull skull = (Skull) block.getState();

        String deathMessage = skull.getPersistentDataContainer().get(new NamespacedKey(this, "deathMessage"), PersistentDataType.STRING);
        if (deathMessage != null) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " discovered remains: " + deathMessage);
        }

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

        Integer xp = skull.getPersistentDataContainer().get(new NamespacedKey(this, "xp"), PersistentDataType.INTEGER);
        if (xp != null) {
            loc.getWorld().spawn(loc, ExperienceOrb.class).setExperience(xp);
        }
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
