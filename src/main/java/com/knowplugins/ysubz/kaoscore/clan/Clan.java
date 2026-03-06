package com.knowplugins.ysubz.kaoscore.clan;

import java.util.*;

public class Clan {

    private String name;
    private String tag;
    private String tagColor;
    private UUID leader;
    private Map<UUID, ClanRole> members;
    private List<UUID> bans;
    private long creationDate;
    private int wins;
    private int losses;
    private int elo;

    public Clan(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag.toUpperCase();
        this.tagColor = "&7";
        this.leader = leader;
        this.members = new HashMap<>();
        this.bans = new ArrayList<>();
        this.creationDate = System.currentTimeMillis();
        this.wins = 0;
        this.losses = 0;
        this.elo = 1000;

        members.put(leader, ClanRole.LEADER);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag.toUpperCase(); }

    public String getTagColor() { return tagColor; }
    public void setTagColor(String color) { this.tagColor = color; }

    public String getFormattedTag() {
        return tagColor + "[" + tag + "]";
    }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) {
        this.leader = leader;
        members.put(leader, ClanRole.LEADER);
    }

    public Map<UUID, ClanRole> getMembers() { return members; }

    public ClanRole getRole(UUID playerId) {
        return members.getOrDefault(playerId, null);
    }

    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }

    public boolean isMod(UUID playerId) {
        ClanRole role = members.get(playerId);
        return role == ClanRole.LEADER || role == ClanRole.MOD;
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public boolean isBanned(UUID playerId) {
        return bans.contains(playerId);
    }

    public void addMember(UUID playerId, ClanRole role) {
        members.put(playerId, role);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void banMember(UUID playerId) {
        bans.add(playerId);
        removeMember(playerId);
    }

    public void unbanMember(UUID playerId) {
        bans.remove(playerId);
    }

    public List<UUID> getBans() { return bans; }

    public long getCreationDate() { return creationDate; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public void addWin() { this.wins++; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public void addLoss() { this.losses++; }

    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }

    public int getMemberCount() { return members.size(); }

    public int getMaxMembers() { return 30; }

    public boolean canInvite() { return members.size() < getMaxMembers(); }
}
