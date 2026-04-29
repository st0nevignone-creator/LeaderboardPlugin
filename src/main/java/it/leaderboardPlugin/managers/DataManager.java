package it.leaderboardPlugin.managers;

import it.leaderboardPlugin.LeaderboardPlugin;
import it.leaderboardPlugin.models.LeaderboardData;
import it.leaderboardPlugin.models.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final LeaderboardPlugin plugin;

    // playerdata.yml
    private File playerFile;
    private FileConfiguration playerData;

    // Cache stat giocatori
    // uuid -> statKey -> value
    private final Map<UUID, Map<String, Double>> stats = new HashMap<>();

    public DataManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        playerFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerFile.exists()) {
            try { playerFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        playerData = YamlConfiguration.loadConfiguration(playerFile);
        loadPlayerData();
    }

    // ============ STAT BUILTIN ============
    public void addKill(UUID uuid)   { addStat(uuid, "kills", 1); }
    public void addDeath(UUID uuid)  { addStat(uuid, "deaths", 1); }
    public void addBlockMined(UUID uuid, int amount) { addStat(uuid, "blocks_mined", amount); }
    public void addTimeOnline(UUID uuid, long minutes) { addStat(uuid, "time_online", minutes); }

    public long getKills(UUID uuid)      { return (long) getStat(uuid, "kills"); }
    public long getDeaths(UUID uuid)     { return (long) getStat(uuid, "deaths"); }
    public double getKD(UUID uuid) {
        long kills  = getKills(uuid);
        long deaths = getDeaths(uuid);
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }
    public long getBlocksMined(UUID uuid) { return (long) getStat(uuid, "blocks_mined"); }
    public long getTimeOnline(UUID uuid)  { return (long) getStat(uuid, "time_online"); }

    // ============ STAT CUSTOM ============
    public void setCustomStat(UUID uuid, String customId, double value) {
        stats.computeIfAbsent(uuid, k -> new HashMap<>()).put("custom_" + customId, value);
        playerData.set("players." + uuid + ".custom_" + customId, value);
        save();
    }

    public void addCustomStat(UUID uuid, String customId, double amount) {
        double current = getStat(uuid, "custom_" + customId);
        setCustomStat(uuid, customId, current + amount);
    }

    public double getCustomStat(UUID uuid, String customId) {
        return getStat(uuid, "custom_" + customId);
    }

    // ============ GENERIC ============
    private void addStat(UUID uuid, String key, double amount) {
        double current = getStat(uuid, key);
        stats.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, current + amount);
        playerData.set("players." + uuid + "." + key, current + amount);
        save();
    }

    public double getStat(UUID uuid, String key) {
        return stats.getOrDefault(uuid, new HashMap<>()).getOrDefault(key, 0.0);
    }

    public double getStatForLeaderboard(UUID uuid, LeaderboardData lb) {
        return switch (lb.getStatType()) {
            case KILLS        -> getKills(uuid);
            case DEATHS       -> getDeaths(uuid);
            case KD           -> getKD(uuid);
            case BLOCKS_MINED -> getBlocksMined(uuid);
            case TIME_ONLINE  -> getTimeOnline(uuid);
            case CUSTOM       -> getCustomStat(uuid, lb.getCustomId());
        };
    }

    public Map<UUID, Map<String, Double>> getAllStats() { return stats; }

    // ============ LEADERBOARDS IN CONFIG ============
    public void saveLeaderboard(LeaderboardData lb) {
        String path = "leaderboards." + lb.getId();
        plugin.getConfig().set(path + ".title",     lb.getTitle());
        plugin.getConfig().set(path + ".stat",      lb.getStatType().name());
        plugin.getConfig().set(path + ".custom-id", lb.getCustomId());
        plugin.getConfig().set(path + ".unit",      lb.getUnit());
        plugin.getConfig().set(path + ".ascending", lb.isAscending());
        Location loc = lb.getLocation();
        if (loc != null) {
            plugin.getConfig().set(path + ".world", loc.getWorld().getName());
            plugin.getConfig().set(path + ".x",     loc.getX());
            plugin.getConfig().set(path + ".y",     loc.getY());
            plugin.getConfig().set(path + ".z",     loc.getZ());
        }
        saveConfig();
    }

    public void deleteLeaderboard(String id) {
        plugin.getConfig().set("leaderboards." + id, null);
        saveConfig();
    }

    public Map<String, LeaderboardData> loadLeaderboards() {
        Map<String, LeaderboardData> map = new HashMap<>();
        var section = plugin.getConfig().getConfigurationSection("leaderboards");
        if (section == null) return map;

        for (String id : section.getKeys(false)) {
            String path = "leaderboards." + id;
            String title    = plugin.getConfig().getString(path + ".title", id);
            String statStr  = plugin.getConfig().getString(path + ".stat", "KILLS");
            String customId = plugin.getConfig().getString(path + ".custom-id", "");
            String unit     = plugin.getConfig().getString(path + ".unit", "");
            boolean asc     = plugin.getConfig().getBoolean(path + ".ascending", false);

            StatType statType;
            try { statType = StatType.valueOf(statStr); } catch (Exception e) { statType = StatType.KILLS; }

            Location loc = null;
            String worldName = plugin.getConfig().getString(path + ".world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = plugin.getConfig().getDouble(path + ".x");
                    double y = plugin.getConfig().getDouble(path + ".y");
                    double z = plugin.getConfig().getDouble(path + ".z");
                    loc = new Location(world, x, y, z);
                }
            }
            map.put(id, new LeaderboardData(id, title, statType, customId, unit, asc, loc));
        }
        return map;
    }

    // ============ PERSISTENCE ============
    private void loadPlayerData() {
        var section = playerData.getConfigurationSection("players");
        if (section == null) return;
        for (String uuidStr : section.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            Map<String, Double> playerStats = new HashMap<>();
            var statSection = playerData.getConfigurationSection("players." + uuidStr);
            if (statSection != null) {
                for (String key : statSection.getKeys(false)) {
                    playerStats.put(key, playerData.getDouble("players." + uuidStr + "." + key));
                }
            }
            stats.put(uuid, playerStats);
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Double>> entry : stats.entrySet()) {
            for (Map.Entry<String, Double> stat : entry.getValue().entrySet()) {
                playerData.set("players." + entry.getKey() + "." + stat.getKey(), stat.getValue());
            }
        }
        save();
    }

    private void save() {
        try { playerData.save(playerFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveConfig() {
        try { plugin.saveConfig(); } catch (Exception e) { e.printStackTrace(); }
    }
}
