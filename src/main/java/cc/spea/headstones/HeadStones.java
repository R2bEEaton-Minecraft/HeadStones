package cc.spea.headstones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

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
        Location loc = event.getEntity().getLocation();
        Block skullBlock = loc.getBlock();
        skullBlock.setType(Material.PLAYER_HEAD);
        Skull skull = (Skull) skullBlock.getState();
        skull.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(event.getEntity().getUniqueId()));
    }


}
