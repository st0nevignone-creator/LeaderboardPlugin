package it.leaderboardPlugin.models;

import org.bukkit.Location;

public class LeaderboardData {

    private final String id;
    private String title;
    private StatType statType;
    private String customId;   // usato solo se statType == CUSTOM
    private String unit;
    private boolean ascending;
    private Location location;

    public LeaderboardData(String id, String title, StatType statType,
                           String customId, String unit, boolean ascending, Location location) {
        this.id        = id;
        this.title     = title;
        this.statType  = statType;
        this.customId  = customId;
        this.unit      = unit;
        this.ascending = ascending;
        this.location  = location;
    }

    public String getId()           { return id; }
    public String getTitle()        { return title; }
    public StatType getStatType()   { return statType; }
    public String getCustomId()     { return customId; }
    public String getUnit()         { return unit; }
    public boolean isAscending()    { return ascending; }
    public Location getLocation()   { return location; }

    public void setTitle(String title)         { this.title = title; }
    public void setStatType(StatType statType) { this.statType = statType; }
    public void setCustomId(String customId)   { this.customId = customId; }
    public void setUnit(String unit)           { this.unit = unit; }
    public void setAscending(boolean asc)      { this.ascending = asc; }
    public void setLocation(Location location) { this.location = location; }
}
