package it.leaderboardPlugin.managers;

import it.leaderboardPlugin.LeaderboardPlugin;
import it.leaderboardPlugin.models.LeaderboardData;
import it.leaderboardPlugin.models.StatType;
import it.leaderboardPlugin.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class HologramManager {

    private final LeaderboardPlugin plugin;
    // leaderboard id -> lista di TextDisplay (una per riga)
    private final Map<String, List<TextDisplay>> holograms = new HashMap<>();

    public HologramManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    // ============ SPAWN / REFRESH ============
    public void spawnOrUpdateHologram(LeaderboardData lb) {
        if (lb.getLocation() == null) return;

        // Rimuovi il vecchio se esiste
        removeHologram(lb.getId());

        List<String> lines = buildLines(lb);
        double lineHeight = plugin.getConfig().getDouble("line-height", 0.28);
        Location baseLoc  = lb.getLocation().clone();

        // Le righe vengono spawmate dall'alto verso il basso
        // La prima riga è la più alta
        double totalHeight = (lines.size() - 1) * lineHeight;
        List<TextDisplay> displays = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = baseLoc.clone().add(0, totalHeight - (i * lineHeight), 0);
            TextDisplay td = spawnTextDisplay(lineLoc, lines.get(i));
            displays.add(td);
        }

        holograms.put(lb.getId(), displays);
    }

    private TextDisplay spawnTextDisplay(Location loc, String text) {
        return loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.text(Utils.color(text));
            td.setBillboard(Display.Billboard.CENTER); // sempre rivolto verso il giocatore
            td.setShadowed(true);
            td.setDefaultBackground(false);
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            // Nessuna hitbox visibile
            td.setPersistent(true);
            Transformation t = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)
            );
            td.setTransformation(t);
        });
    }

    // ============ BUILD LINES ============
    private List<String> buildLines(LeaderboardData lb) {
        List<String> lines = new ArrayList<>();
        int topSize = plugin.getConfig().getInt("top-size", 10);

        // Titolo
        String titleFmt = plugin.getConfig().getString("formats.title", "&6&l✦ {title} ✦");
        lines.add(titleFmt.replace("{title}", lb.getTitle()));

        // Separatore
        lines.add("&8&m──────────────");

        // Raccoglie e ordina i giocatori
        Map<UUID, Double> values = new HashMap<>();
        for (UUID uuid : plugin.getDataManager().getAllStats().keySet()) {
            double val = plugin.getDataManager().getStatForLeaderboard(uuid, lb);
            values.put(uuid, val);
        }

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(values.entrySet());
        sorted.sort(lb.isAscending()
                ? Map.Entry.comparingByValue()
                : Map.Entry.<UUID, Double>comparingByValue().reversed());

        if (sorted.isEmpty()) {
            String emptyFmt = plugin.getConfig().getString("formats.empty", "&8Nessun dato ancora.");
            lines.add(emptyFmt);
        } else {
            int count = Math.min(topSize, sorted.size());
            for (int i = 0; i < count; i++) {
                Map.Entry<UUID, Double> entry = sorted.get(i);
                String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (playerName == null) playerName = "Unknown";
                String valueStr = formatValue(lb, entry.getValue());

                String fmt;
                if      (i == 0) fmt = plugin.getConfig().getString("formats.first",   "&6#1 &e{player} &7- &f{value}");
                else if (i == 1) fmt = plugin.getConfig().getString("formats.second",  "&7#2 &f{player} &7- &f{value}");
                else if (i == 2) fmt = plugin.getConfig().getString("formats.third",   "&c#3 &f{player} &7- &f{value}");
                else             fmt = plugin.getConfig().getString("formats.default",  "&8#{pos} &7{player} &8- &7{value}");

                lines.add(fmt
                        .replace("{pos}",    String.valueOf(i + 1))
                        .replace("{player}", playerName)
                        .replace("{value}",  valueStr));
            }
        }

        // Footer
        String footer = plugin.getConfig().getString("formats.footer", "&8Aggiornato ogni ora");
        lines.add("&8&m──────────────");
        lines.add(footer);

        return lines;
    }

    private String formatValue(LeaderboardData lb, double value) {
        String unit = lb.getUnit() != null ? " " + lb.getUnit() : "";
        if (lb.getStatType() == StatType.KD) {
            return Utils.formatKD(value) + unit;
        }
        if (lb.getStatType() == StatType.TIME_ONLINE) {
            return Utils.formatTime((long) value);
        }
        return String.valueOf((long) value) + unit;
    }

    // ============ REMOVE ============
    public void removeHologram(String id) {
        List<TextDisplay> old = holograms.remove(id);
        if (old != null) old.forEach(Entity::remove);
    }

    public void removeAll() {
        holograms.values().forEach(list -> list.forEach(Entity::remove));
        holograms.clear();
    }

    // ============ UPDATE ALL ============
    public void updateAll(Map<String, LeaderboardData> leaderboards) {
        for (LeaderboardData lb : leaderboards.values()) {
            spawnOrUpdateHologram(lb);
        }
    }

    public boolean hasHologram(String id) { return holograms.containsKey(id); }
}
