package it.leaderboardPlugin.listeners;

import it.leaderboardPlugin.LeaderboardPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsListener implements Listener {

    private final LeaderboardPlugin plugin;
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public StatsListener(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        plugin.getDataManager().addDeath(victim.getUniqueId());

        Player killer = victim.getKiller();
        if (killer != null && killer != victim) {
            plugin.getDataManager().addKill(killer.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getDataManager().addBlockMined(event.getPlayer().getUniqueId(), 1);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        joinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Long joinTime = joinTimes.remove(uuid);
        if (joinTime != null) {
            long minutes = (System.currentTimeMillis() - joinTime) / 60000;
            if (minutes > 0) plugin.getDataManager().addTimeOnline(uuid, minutes);
        }
    }
}
