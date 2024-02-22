package cc.spea.headstones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeadStones extends JavaPlugin implements Listener {
    public HashMap<String, List<ItemStack>> graveItems = new HashMap<>();
    public HashMap<String, Integer> graveExp = new HashMap<>();

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
        skullBlock.setType(Material.PLAYER_HEAD);
        Skull skull = (Skull) skullBlock.getState();

        String key = event.getEntity().getUniqueId() + " " + skullBlock.getLocation();
        graveItems.put(key, List.copyOf(event.getDrops()));
        graveExp.put(key, event.getDroppedExp());

        event.getDrops().clear();
        event.setDroppedExp(0);

        skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(event.getEntity().getUniqueId()));
        skull.update();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        Skull skull = (Skull) event.getBlock().getState();
        String key = skull.getOwningPlayer().getUniqueId() + " " + loc;
        if (graveItems.containsKey(key)) {
            event.setDropItems(false);
            for (ItemStack is : graveItems.get(key)) {
                loc.getWorld().dropItemNaturally(loc, is);
            }
            graveItems.remove(key);
        }
        if (graveExp.containsKey(key)) {
            loc.getWorld().spawn(loc, ExperienceOrb.class).setExperience(graveExp.get(key));
            graveExp.remove(key);
        }
    }
}
