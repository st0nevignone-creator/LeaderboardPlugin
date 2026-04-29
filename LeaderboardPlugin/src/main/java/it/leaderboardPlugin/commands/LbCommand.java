package it.leaderboardPlugin.commands;

import it.leaderboardPlugin.LeaderboardPlugin;
import it.leaderboardPlugin.models.LeaderboardData;
import it.leaderboardPlugin.models.StatType;
import it.leaderboardPlugin.utils.Utils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LbCommand implements CommandExecutor {

    private final LeaderboardPlugin plugin;

    public LbCommand(LeaderboardPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /lb create <id> <stat> <titolo...>
            // es: /lb create kills_top KILLS Top Kills
            // es: /lb create kd_top KD Top K/D Ratio
            // es: /lb create punti CUSTOM:punteggi Top Punteggi
            case "create" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player player)) { onlyPlayer(sender); return true; }
                if (args.length < 3) {
                    sender.sendMessage(Utils.colorStr("&cUso: /lb create <id> <stat> [titolo...]"));
                    sender.sendMessage(Utils.colorStr("&7Stats: &fKILLS, DEATHS, KD, BLOCKS_MINED, TIME_ONLINE, CUSTOM:<id>"));
                    return true;
                }

                String id      = args[1].toLowerCase();
                String statArg = args[2].toUpperCase();
                String title   = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : id;

                if (plugin.getLeaderboardManager().exists(id)) {
                    sender.sendMessage(Utils.colorStr("&cEsiste già una classifica con id &e" + id + "&c."));
                    return true;
                }

                StatType statType;
                String customId  = "";
                String unit      = "";
                boolean ascending = false;

                if (statArg.startsWith("CUSTOM:")) {
                    statType = StatType.CUSTOM;
                    customId = statArg.substring(7).toLowerCase();
                    unit     = "";
                } else {
                    try { statType = StatType.valueOf(statArg); }
                    catch (IllegalArgumentException e) {
                        sender.sendMessage(Utils.colorStr("&cStat non valida: &e" + statArg));
                        sender.sendMessage(Utils.colorStr("&7Valide: KILLS, DEATHS, KD, BLOCKS_MINED, TIME_ONLINE, CUSTOM:<id>"));
                        return true;
                    }
                    unit      = switch (statType) {
                        case KILLS        -> "kill";
                        case DEATHS       -> "morti";
                        case KD           -> "";
                        case BLOCKS_MINED -> "blocchi";
                        case TIME_ONLINE  -> "";
                        default           -> "";
                    };
                    ascending = (statType == StatType.DEATHS);
                }

                plugin.getLeaderboardManager().create(id, title, statType, customId, unit, ascending,
                        player.getLocation());

                sender.sendMessage(Utils.colorStr("&aClassifica &e" + id + " &acreata alla tua posizione!"));
                sender.sendMessage(Utils.colorStr("&7Usa &e/lb update " + id + " &7per aggiornarla manualmente."));
            }

            // /lb delete <id>
            case "delete" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                if (args.length < 2) { sender.sendMessage(Utils.colorStr("&cUso: /lb delete <id>")); return true; }
                String id = args[1].toLowerCase();
                if (plugin.getLeaderboardManager().delete(id)) {
                    sender.sendMessage(Utils.colorStr("&aClassifica &e" + id + " &aeliminata."));
                } else {
                    sender.sendMessage(Utils.colorStr("&cClassifica non trovata: &e" + id));
                }
            }

            // /lb list
            case "list" -> {
                var all = plugin.getLeaderboardManager().getAll();
                if (all.isEmpty()) {
                    sender.sendMessage(Utils.colorStr("&7Nessuna classifica creata."));
                    return true;
                }
                sender.sendMessage(Utils.colorStr("&6&l── Classifiche ──"));
                for (LeaderboardData lb : all.values()) {
                    String loc = lb.getLocation() != null
                            ? lb.getLocation().getWorld().getName()
                              + " " + (int)lb.getLocation().getX()
                              + " " + (int)lb.getLocation().getY()
                              + " " + (int)lb.getLocation().getZ()
                            : "nessuna posizione";
                    sender.sendMessage(Utils.colorStr("&e" + lb.getId()
                            + " &8| &7" + lb.getTitle()
                            + " &8| &f" + lb.getStatType().name()
                            + " &8| &7" + loc));
                }
            }

            // /lb update [id]
            case "update" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                if (args.length >= 2) {
                    String id = args[1].toLowerCase();
                    if (!plugin.getLeaderboardManager().exists(id)) {
                        sender.sendMessage(Utils.colorStr("&cClassifica non trovata: &e" + id));
                        return true;
                    }
                    plugin.getLeaderboardManager().updateSingle(id);
                    sender.sendMessage(Utils.colorStr("&aClassifica &e" + id + " &aaggiornata!"));
                } else {
                    plugin.getLeaderboardManager().updateAll();
                    sender.sendMessage(Utils.colorStr("&aTutte le classifiche aggiornate!"));
                }
            }

            // /lb setvalue <player> <stat|CUSTOM:id> <valore>
            case "setvalue" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                if (args.length < 4) {
                    sender.sendMessage(Utils.colorStr("&cUso: /lb setvalue <player> <stat|CUSTOM:id> <valore>"));
                    return true;
                }
                var target = plugin.getServer().getOfflinePlayer(args[1]);
                UUID uuid  = target.getUniqueId();
                String statArg = args[2].toUpperCase();
                double value;
                try { value = Double.parseDouble(args[3]); }
                catch (NumberFormatException e) { sender.sendMessage(Utils.colorStr("&cValore non valido.")); return true; }

                if (statArg.startsWith("CUSTOM:")) {
                    String customId = statArg.substring(7).toLowerCase();
                    plugin.getDataManager().setCustomStat(uuid, customId, value);
                    sender.sendMessage(Utils.colorStr("&aImposto &e" + args[1] + " &a→ custom:" + customId + " = &f" + value));
                } else {
                    sender.sendMessage(Utils.colorStr("&cUsa CUSTOM:<id> per le stat custom. Le stat base si aggiornano automaticamente."));
                    return true;
                }
                plugin.getLeaderboardManager().updateAll();
            }

            // /lb addvalue <player> <CUSTOM:id> <quantità>
            case "addvalue" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                if (args.length < 4) {
                    sender.sendMessage(Utils.colorStr("&cUso: /lb addvalue <player> <CUSTOM:id> <quantità>"));
                    return true;
                }
                var target = plugin.getServer().getOfflinePlayer(args[1]);
                String statArg = args[2].toUpperCase();
                double amount;
                try { amount = Double.parseDouble(args[3]); }
                catch (NumberFormatException e) { sender.sendMessage(Utils.colorStr("&cValore non valido.")); return true; }

                if (statArg.startsWith("CUSTOM:")) {
                    String customId = statArg.substring(7).toLowerCase();
                    plugin.getDataManager().addCustomStat(target.getUniqueId(), customId, amount);
                    sender.sendMessage(Utils.colorStr("&aAggiunto &f" + amount + " &aa &e" + args[1] + " &a→ custom:" + customId));
                } else {
                    sender.sendMessage(Utils.colorStr("&cSolo le stat CUSTOM:<id> possono essere modificate manualmente."));
                }
                plugin.getLeaderboardManager().updateAll();
            }

            // /lb reload
            case "reload" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                plugin.getHologramManager().removeAll();
                plugin.reloadConfig();
                plugin.getLeaderboardManager().loadAll();
                sender.sendMessage(Utils.colorStr("&aLeaderboardPlugin ricaricato!"));
            }

            // /lb move <id>  → sposta ologramma alla posizione attuale
            case "move" -> {
                if (!sender.hasPermission("leaderboard.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player player)) { onlyPlayer(sender); return true; }
                if (args.length < 2) { sender.sendMessage(Utils.colorStr("&cUso: /lb move <id>")); return true; }
                String id = args[1].toLowerCase();
                LeaderboardData lb = plugin.getLeaderboardManager().get(id);
                if (lb == null) { sender.sendMessage(Utils.colorStr("&cClassifica non trovata.")); return true; }
                lb.setLocation(player.getLocation());
                plugin.getDataManager().saveLeaderboard(lb);
                plugin.getHologramManager().spawnOrUpdateHologram(lb);
                sender.sendMessage(Utils.colorStr("&aOlogramma &e" + id + " &aspostato qui!"));
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(Utils.colorStr("&6&l── LeaderboardPlugin ──"));
        s.sendMessage(Utils.colorStr("&e/lb create <id> <stat> [titolo] &8- &7Crea classifica alla tua pos"));
        s.sendMessage(Utils.colorStr("&e/lb delete <id>               &8- &7Elimina classifica"));
        s.sendMessage(Utils.colorStr("&e/lb list                       &8- &7Lista classifiche"));
        s.sendMessage(Utils.colorStr("&e/lb update [id]                &8- &7Aggiorna ologrammi"));
        s.sendMessage(Utils.colorStr("&e/lb move <id>                  &8- &7Sposta ologramma qui"));
        s.sendMessage(Utils.colorStr("&e/lb setvalue <pl> <CUSTOM:id> <val> &8- &7Imposta valore custom"));
        s.sendMessage(Utils.colorStr("&e/lb addvalue <pl> <CUSTOM:id> <val> &8- &7Aggiunge valore custom"));
        s.sendMessage(Utils.colorStr("&e/lb reload                     &8- &7Ricarica config"));
        s.sendMessage(Utils.colorStr("&7Stats disponibili: &fKILLS, DEATHS, KD, BLOCKS_MINED, TIME_ONLINE, CUSTOM:<id>"));
    }

    private void noPerms(CommandSender s)  { s.sendMessage(Utils.colorStr("&cPermesso negato.")); }
    private void onlyPlayer(CommandSender s) { s.sendMessage(Utils.colorStr("&cSolo i giocatori possono usare questo comando.")); }
}
