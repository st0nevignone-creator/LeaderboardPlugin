package it.leaderboardPlugin;

import it.leaderboardPlugin.commands.LbCommand;
import it.leaderboardPlugin.listeners.StatsListener;
import it.leaderboardPlugin.managers.DataManager;
import it.leaderboardPlugin.managers.HologramManager;
import it.leaderboardPlugin.managers.LeaderboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeaderboardPlugin extends JavaPlugin {

    private static LeaderboardPlugin instance;
    private DataManager dataManager;
    private HologramManager hologramManager;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Managers
        this.dataManager       = new DataManager(this);
        this.hologramManager   = new HologramManager(this);
        this.leaderboardManager = new LeaderboardManager(this);

        // Carica classifiche salvate e spawna ologrammi
        leaderboardManager.loadAll();

        // Comando
        getCommand("lb").setExecutor(new LbCommand(this));

        // Listener stats
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);

        // Aggiornamento automatico ogni X minuti (default 60)
        long intervalMinutes = getConfig().getLong("update-interval-minutes", 60);
        long intervalTicks   = intervalMinutes * 60 * 20; // minuti → ticks
        getServer().getScheduler().runTaskTimer(this, () -> {
            leaderboardManager.updateAll();
            getLogger().info("Classifiche aggiornate automaticamente.");
        }, intervalTicks, intervalTicks);

        getLogger().info("LeaderboardPlugin abilitato!");
    }

    @Override
    public void onDisable() {
        // Salva i dati dei giocatori ancora online
        for (var player : getServer().getOnlinePlayers()) {
            // Il listener onQuit non viene chiamato su disable, lo gestiamo qui
        }
        dataManager.saveAll();

        // Rimuovi gli ologrammi dal mondo (verranno ricreati al prossimo avvio)
        hologramManager.removeAll();

        getLogger().info("LeaderboardPlugin disabilitato.");
    }

    public static LeaderboardPlugin getInstance()          { return instance; }
    public DataManager getDataManager()                    { return dataManager; }
    public HologramManager getHologramManager()            { return hologramManager; }
    public LeaderboardManager getLeaderboardManager()      { return leaderboardManager; }
}
