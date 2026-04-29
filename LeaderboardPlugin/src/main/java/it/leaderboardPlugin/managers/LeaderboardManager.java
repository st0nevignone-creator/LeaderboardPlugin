package it.leaderboardPlugin.managers;

import it.leaderboardPlugin.LeaderboardPlugin;
import it.leaderboardPlugin.models.LeaderboardData;
import it.leaderboardPlugin.models.StatType;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardManager {

    private final LeaderboardPlugin plugin;
    private final Map<String, LeaderboardData> leaderboards = new HashMap<>();

    public LeaderboardManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        leaderboards.clear();
        leaderboards.putAll(plugin.getDataManager().loadLeaderboards());
        // Spawna gli ologrammi per quelli già salvati
        for (LeaderboardData lb : leaderboards.values()) {
            if (lb.getLocation() != null) {
                plugin.getHologramManager().spawnOrUpdateHologram(lb);
            }
        }
    }

    public boolean create(String id, String title, StatType statType,
                          String customId, String unit, boolean ascending, Location loc) {
        if (leaderboards.containsKey(id)) return false;
        LeaderboardData lb = new LeaderboardData(id, title, statType, customId, unit, ascending, loc);
        leaderboards.put(id, lb);
        plugin.getDataManager().saveLeaderboard(lb);
        plugin.getHologramManager().spawnOrUpdateHologram(lb);
        return true;
    }

    public boolean delete(String id) {
        if (!leaderboards.containsKey(id)) return false;
        leaderboards.remove(id);
        plugin.getDataManager().deleteLeaderboard(id);
        plugin.getHologramManager().removeHologram(id);
        return true;
    }

    public void updateAll() {
        plugin.getHologramManager().updateAll(leaderboards);
    }

    public void updateSingle(String id) {
        LeaderboardData lb = leaderboards.get(id);
        if (lb != null) plugin.getHologramManager().spawnOrUpdateHologram(lb);
    }

    public LeaderboardData get(String id)              { return leaderboards.get(id); }
    public Map<String, LeaderboardData> getAll()       { return leaderboards; }
    public boolean exists(String id)                   { return leaderboards.containsKey(id); }
}
