package it.leaderboardPlugin.managers;

import it.leaderboardPlugin.LeaderboardPlugin;
import it.leaderboardPlugin.models.LeaderboardData;
import it.leaderboardPlugin.models.StatType;
import it.leaderboardPlugin.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class HologramManager {

    private final LeaderboardPlugin plugin;
    private final Map<String, List<TextDisplay>> holograms = new HashMap<>();
    private static final String PDC_KEY = "lb_hologram_id";

    public HologramManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public void removeOrphanedHolograms() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByType(EntityType.TEXT_DISPLAY)) {
                NamespacedKey key = new NamespacedKey(plugin, PDC_KEY);
                if (entity.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeOrphanedById(String lbId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByType(EntityType.TEXT_DISPLAY)) {
                NamespacedKey key = new NamespacedKey(plugin, PDC_KEY);
                String storedId = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (lbId.equals(storedId)) entity.remove();
            }
        }
    }

    public void spawnOrUpdateHologram(LeaderboardData lb) {
        if (lb.getLocation() == null) return;
        removeHologram(lb.getId());
        removeOrphanedById(lb.getId());

        List<String> lines = buildLines(lb);
        double lineHeight = plugin.getConfig().getDouble("line-height", 0.28);
        Location baseLoc = lb.getLocation().clone();
        double totalHeight = (lines.size() - 1) * lineHeight;
        List<TextDisplay> displays = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = baseLoc.clone().add(0, totalHeight - (i * lineHeight), 0);
            displays.add(spawnTextDisplay(lineLoc, lines.get(i), lb.getId()));
        }
        holograms.put(lb.getId(), displays);
    }

    private TextDisplay spawnTextDisplay(Location loc, String text, String lbId) {
        return loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.text(Utils.color(text));
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowed(true);
            td.setDefaultBackground(false);
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setPersistent(false);

            NamespacedKey key = new NamespacedKey(plugin, PDC_KEY);
            td.getPersistentDataContainer().set(key, PersistentDataType.STRING, lbId);

            td.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });
    }

    private List<String> buildLines(LeaderboardData lb) {
        List<String> lines = new ArrayList<>();
        int topSize = plugin.getConfig().getInt("top-size", 10);

        String titleFmt = plugin.getConfig().getString("formats.title", "&6&l✦ {title} ✦");
        lines.add(titleFmt.replace("{title}", lb.getTitle()));
        lines.add("&8&m──────────────");

        Map<UUID, Double> values = new HashMap<>();
        for (UUID uuid : plugin.getDataManager().getAllStats().keySet()) {
            values.put(uuid, plugin.getDataManager().getStatForLeaderboard(uuid, lb));
        }

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(values.entrySet());
        sorted.sort(lb.isAscending()
                ? Map.Entry.comparingByValue()
                : Map.Entry.<UUID, Double>comparingByValue().reversed());

        if (sorted.isEmpty()) {
            lines.add(plugin.getConfig().getString("formats.empty", "&8Nessun dato ancora."));
        } else {
            int count = Math.min(topSize, sorted.size());
            for (int i = 0; i < count; i++) {
                Map.Entry<UUID, Double> entry = sorted.get(i);
                String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (playerName == null) playerName = "Unknown";
                String valueStr = formatValue(lb, entry.getValue());

                String fmt;
                if      (i == 0) fmt = plugin.getConfig().getString("formats.first",  "&6#1 &e{player} &7- &f{value}");
                else if (i == 1) fmt = plugin.getConfig().getString("formats.second", "&7#2 &f{player} &7- &f{value}");
                else if (i == 2) fmt = plugin.getConfig().getString("formats.third",  "&c#3 &f{player} &7- &f{value}");
                else             fmt = plugin.getConfig().getString("formats.default","&8#{pos} &7{player} &8- &7{value}");

                lines.add(fmt
                        .replace("{pos}",    String.valueOf(i + 1))
                        .replace("{player}", playerName)
                        .replace("{value}",  valueStr));
            }
        }

        lines.add("&8&m──────────────");
        lines.add(plugin.getConfig().getString("formats.footer", "&8Aggiornato ogni ora"));
        return lines;
    }

    private String formatValue(LeaderboardData lb, double value) {
        String unit = lb.getUnit() != null && !lb.getUnit().isEmpty() ? " " + lb.getUnit() : "";
        if (lb.getStatType() == StatType.KD)          return Utils.formatKD(value) + unit;
        if (lb.getStatType() == StatType.TIME_ONLINE)  return Utils.formatTime((long) value);
        return (long) value + unit;
    }

    public void removeHologram(String id) {
        List<TextDisplay> old = holograms.remove(id);
        if (old != null) old.forEach(Entity::remove);
        removeOrphanedById(id);
    }

    public void removeAll() {
        holograms.values().forEach(list -> list.forEach(Entity::remove));
        holograms.clear();
        removeOrphanedHolograms();
    }

    public void updateAll(Map<String, LeaderboardData> leaderboards) {
        for (LeaderboardData lb : leaderboards.values()) spawnOrUpdateHologram(lb);
    }

    public boolean hasHologram(String id) { return holograms.containsKey(id); }
}
