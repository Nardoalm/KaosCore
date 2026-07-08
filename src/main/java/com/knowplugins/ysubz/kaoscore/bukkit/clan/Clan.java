package com.knowplugins.ysubz.kaoscore.bukkit.clan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

public class Clan {

    public static final String ANIMATED_TAG_COLOR = "wave";
    private static final List<String> ANIMATED_TAG_PALETTE = Arrays.asList(
            "§c", "§6", "§e", "§a", "§b", "§9", "§d"
    );

    private String name;
    private String tag;
    private String tagColor;
    private UUID leader;
    private final Map<UUID, ClanRole> members;
    private final List<UUID> bans;
    private long creationDate;
    private int wins;
    private int losses;
    private int elo;

    public Clan(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag.toUpperCase();
        this.tagColor = "§7";
        this.leader = leader;
        this.members = new HashMap<>();
        this.bans = new ArrayList<>();
        this.creationDate = System.currentTimeMillis();
        this.wins = 0;
        this.losses = 0;
        this.elo = 1000;

        members.put(leader, ClanRole.LEADER);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag.toUpperCase();
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String color) {
        this.tagColor = color == null || color.trim().isEmpty() ? "§7" : color.trim();
    }

    public String getFormattedTag() {
        if (isAnimatedTagColor()) {
            return buildAnimatedTag();
        }
        if ("brazil".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§a", "§e") + "§f]";
        }
        if ("usa".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildThreeColorTag("§9", "§f", "§c") + "§f]";
        }
        if ("argentina".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§b", "§f") + "§f]";
        }
        if ("na".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§9", "§c") + "§f]";
        }
        if ("sa".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§a", "§e") + "§f]";
        }
        if ("germany".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§c", "§6") + "§f]";
        }
        return tagColor + "[" + tag + "]";
    }

    public String getNametagFormattedTag() {
        if (isAnimatedTagColor()) {
            return "§f[" + getAnimatedPrimaryColor() + tag + "§f]";
        }
        if ("brazil".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§a", "§e") + "§f]";
        }
        if ("usa".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildThreeColorTag("§9", "§f", "§c") + "§f]";
        }
        if ("argentina".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§b", "§f") + "§f]";
        }
        if ("na".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§9", "§c") + "§f]";
        }
        if ("sa".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§a", "§e") + "§f]";
        }
        if ("germany".equalsIgnoreCase(tagColor)) {
            return "§f[" + buildTwoColorTag("§c", "§6") + "§f]";
        }
        return tagColor + "[" + tag + "]";
    }

    private String buildTwoColorTag(String color1, String color2) {
        String safeTag = tag == null ? "" : tag.toUpperCase(Locale.ROOT);
        if (safeTag.isEmpty()) {
            return "";
        }
        int mid = (safeTag.length() + 1) / 2;
        String first = safeTag.substring(0, mid);
        String second = safeTag.substring(mid);
        return color1 + first + color2 + second;
    }

    private String buildThreeColorTag(String color1, String color2, String color3) {
        String safeTag = tag == null ? "" : tag.toUpperCase(Locale.ROOT);
        if (safeTag.isEmpty()) {
            return "";
        }
        int len = safeTag.length();
        // Split into 3 roughly equal parts; middle gets the extra char if not divisible by 3
        int first = len / 3;
        int last = len / 3;
        int mid = len - first - last;
        String p1 = safeTag.substring(0, first);
        String p2 = safeTag.substring(first, first + mid);
        String p3 = safeTag.substring(first + mid);
        if (first == 0) {
            // Very short tag: just blue/red split
            return buildTwoColorTag(color1, color3);
        }
        return color1 + p1 + color2 + p2 + color3 + p3;
    }

    public boolean isAnimatedTagColor() {
        return ANIMATED_TAG_COLOR.equalsIgnoreCase(tagColor == null ? "" : tagColor.trim());
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
        members.put(leader, ClanRole.LEADER);
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
        return role == ClanRole.LEADER || role == ClanRole.ADMIN;
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

    public List<UUID> getBans() {
        return bans;
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

    private String buildAnimatedTag() {
        String safeTag = tag == null ? "" : tag.toUpperCase(Locale.ROOT);
        if (safeTag.isEmpty()) {
            return "§f[]";
        }

        long offset = System.currentTimeMillis() / 150L;
        StringBuilder builder = new StringBuilder("§f[");
        for (int i = 0; i < safeTag.length(); i++) {
            builder.append(resolveAnimatedColor(i + offset)).append(safeTag.charAt(i));
        }
        builder.append("§f]");
        return builder.toString();
    }

    private String getAnimatedPrimaryColor() {
        return resolveAnimatedColor(System.currentTimeMillis() / 150L);
    }

    private String resolveAnimatedColor(long index) {
        int paletteIndex = (int) Math.floorMod(index, (long) ANIMATED_TAG_PALETTE.size());
        return ANIMATED_TAG_PALETTE.get(paletteIndex);
    }
}