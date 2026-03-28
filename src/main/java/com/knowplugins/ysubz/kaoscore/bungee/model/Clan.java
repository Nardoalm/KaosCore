package com.knowplugins.ysubz.kaoscore.bungee;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Clan {

    private final Map<UUID, ClanRole> members = new HashMap<>();
    private String name;
    private String tag;
    private String tagColor = "§7";
    private UUID leader;
    private long creationDate = System.currentTimeMillis();
    private int wins;
    private int losses;
    private int elo = 1000;

    public Clan(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag == null ? "" : tag.toUpperCase(Locale.ROOT);
        this.leader = leader;
        if (leader != null) {
            members.put(leader, ClanRole.LEADER);
        }
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag == null ? "" : tag.toUpperCase(Locale.ROOT);
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor == null ? "§7" : tagColor;
    }

    public String getFormattedTag() {
        return getTagColor() + "[" + getTag() + "]";
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
        if (leader != null) {
            members.put(leader, ClanRole.LEADER);
        }
    }

    public Map<UUID, ClanRole> getMembers() {
        return members;
    }

    public ClanRole getRole(UUID playerId) {
        return members.get(playerId);
    }

    public boolean isLeader(UUID playerId) {
        return leader != null && leader.equals(playerId);
    }

    public boolean isMod(UUID playerId) {
        ClanRole role = members.get(playerId);
        return role == ClanRole.LEADER || role == ClanRole.MOD;
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public void addMember(UUID playerId, ClanRole role) {
        members.put(playerId, role);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void addWin() {
        this.wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void addLoss() {
        this.losses++;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getMaxMembers() {
        return 30;
    }

    public boolean canInvite() {
        return members.size() < getMaxMembers();
    }
}


