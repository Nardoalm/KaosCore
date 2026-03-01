package com.knowplugins.nardodev.knowpractice.managers;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.knowplugins.nardodev.knowpractice.clan.Clan;
import com.knowplugins.nardodev.knowpractice.clan.ClanRole;
import com.knowplugins.nardodev.knowpractice.clan.ClanInvite;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClanManager {

    private final KnowPractice plugin;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerClans;
    private final Map<UUID, List<ClanInvite>> invites;
    private final List<String> tagColors;

    public ClanManager(KnowPractice plugin) {
        this.plugin = plugin;
        this.clans = new ConcurrentHashMap<>();
        this.playerClans = new ConcurrentHashMap<>();
        this.invites = new ConcurrentHashMap<>();
        this.tagColors = new ArrayList<>();
        loadTagColors();
    }

    private void loadTagColors() {
        tagColors.addAll(plugin.getConfig().getStringList("clan.tag-colors"));
        if (tagColors.isEmpty()) {
            tagColors.addAll(Arrays.asList(
                    "&0Preto", "&1Azul Escuro", "&2Verde Escuro", "&3Azul Turquesa",
                    "&4Vermelho Escuro", "&5Roxo", "&6Dourado", "&7Cinza",
                    "&8Cinza Escuro", "&9Azul", "&aVerde", "&bAqua",
                    "&cVermelho", "&dRoxo Claro", "&eAmarelo", "&fBranco",
                    "&7Cinza Po", "&bAzul Claro", "&dRosa", "&6Laranja"
            ));
        }
    }

    public void loadClans() {
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) return;

        clans.clear();
        playerClans.clear();

        String selectClans = "SELECT * FROM clans";
        String selectMembers = "SELECT * FROM clan_members WHERE clan_name = ?";

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectClans);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String tag = rs.getString("tag");
                String tagColor = rs.getString("tag_color");
                UUID leader = UUID.fromString(rs.getString("leader"));

                Clan clan = new Clan(name, tag, leader);
                clan.setTagColor(tagColor);

                try (PreparedStatement memberStmt = conn.prepareStatement(selectMembers)) {
                    memberStmt.setString(1, name);
                    try (ResultSet memberRs = memberStmt.executeQuery()) {
                        while (memberRs.next()) {
                            UUID uuid = UUID.fromString(memberRs.getString("player_uuid"));
                            String roleName = memberRs.getString("role");
                            ClanRole role = ClanRole.valueOf(roleName);
                            clan.getMembers().put(uuid, role);
                            playerClans.put(uuid, name);
                        }
                    }
                }

                clans.put(name.toLowerCase(), clan);
            }

            System.out.println("\u001B[32m[KnowPractice] " + clans.size() + " clans carregados\u001B[0m");

        } catch (SQLException e) {
            System.out.println("\u001B[31m[KnowPractice] Erro ao carregar clans: " + e.getMessage() + "\u001B[0m");
        }
    }

    public void saveClans() {
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) return;

        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
    }

    public void saveClan(Clan clan) {
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) return;

        String clanSql = "INSERT INTO clans (name, tag, tag_color, leader, created) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE tag = ?, tag_color = ?, leader = ?";

        String memberSql = "INSERT INTO clan_members (clan_name, player_uuid, player_name, role, joined) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE role = ?, player_name = ?";

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(clanSql)) {

            stmt.setString(1, clan.getName());
            stmt.setString(2, clan.getTag());
            stmt.setString(3, clan.getTagColor());
            stmt.setString(4, clan.getLeader().toString());
            stmt.setLong(5, clan.getCreationDate());
            stmt.setString(6, clan.getTag());
            stmt.setString(7, clan.getTagColor());
            stmt.setString(8, clan.getLeader().toString());
            stmt.executeUpdate();

            try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
                    String playerName = "Unknown";
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        playerName = player.getName();
                    }

                    memberStmt.setString(1, clan.getName());
                    memberStmt.setString(2, entry.getKey().toString());
                    memberStmt.setString(3, playerName);
                    memberStmt.setString(4, entry.getValue().name());
                    memberStmt.setLong(5, System.currentTimeMillis());
                    memberStmt.setString(6, entry.getValue().name());
                    memberStmt.setString(7, playerName);
                    memberStmt.addBatch();
                }
                memberStmt.executeBatch();
            }

        } catch (SQLException e) {
            System.out.println("\u001B[31m[KnowPractice] Erro ao salvar clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    public boolean createClan(String name, String tag, Player leader) {
        if (clans.containsKey(name.toLowerCase())) return false;
        if (playerClans.containsKey(leader.getUniqueId())) return false;

        Clan clan = new Clan(name, tag, leader.getUniqueId());
        clans.put(name.toLowerCase(), clan);
        playerClans.put(leader.getUniqueId(), name);
        saveClan(clan);

        System.out.println("\u001B[32m[KnowPractice] Clan criado: " + name + " [" + tag + "] por " + leader.getName() + "\u001B[0m");
        return true;
    }

    public boolean deleteClan(String name, Player deleter) {
        Clan clan = clans.get(name.toLowerCase());
        if (clan == null) return false;

        if (!clan.isLeader(deleter.getUniqueId())) return false;

        for (UUID member : clan.getMembers().keySet()) {
            playerClans.remove(member);
        }

        clans.remove(name.toLowerCase());

        if (Managers.getDatabaseManager() != null && Managers.getDatabaseManager().isConnected()) {
            String deleteClanSql = "DELETE FROM clans WHERE name = ?";
            String deleteMembersSql = "DELETE FROM clan_members WHERE clan_name = ?";
            try (Connection conn = Managers.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteClanSql);
                 PreparedStatement memberStmt = conn.prepareStatement(deleteMembersSql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
                memberStmt.setString(1, name);
                memberStmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("\u001B[31m[KnowPractice] Erro ao deletar clan: " + e.getMessage() + "\u001B[0m");
            }
        }

        System.out.println("\u001B[33m[KnowPractice] Clan deletado: " + name + " por " + deleter.getName() + "\u001B[0m");
        return true;
    }

    public Clan getClan(String name) {
        return clans.get(name.toLowerCase());
    }

    public Clan getPlayerClan(UUID playerId) {
        String clanName = playerClans.get(playerId);
        if (clanName == null) return null;
        return clans.get(clanName.toLowerCase());
    }

    public boolean isInClan(UUID playerId) {
        return playerClans.containsKey(playerId);
    }

    public void addMember(Clan clan, UUID playerId, ClanRole role) {
        clan.addMember(playerId, role);
        playerClans.put(playerId, clan.getName());
        saveClan(clan);
    }

    public void removeMember(Clan clan, UUID playerId) {
        clan.removeMember(playerId);
        playerClans.remove(playerId);
        saveClan(clan);
    }

    public void promote(Clan clan, UUID playerId) {
        ClanRole current = clan.getRole(playerId);
        if (current == null) return;

        ClanRole newRole;
        if (current == ClanRole.RECRUIT) newRole = ClanRole.MEMBER;
        else if (current == ClanRole.MEMBER) newRole = ClanRole.MOD;
        else return;

        clan.getMembers().put(playerId, newRole);
        saveClan(clan);
    }

    public void demote(Clan clan, UUID playerId) {
        ClanRole current = clan.getRole(playerId);
        if (current == null) return;

        ClanRole newRole;
        if (current == ClanRole.MOD) newRole = ClanRole.MEMBER;
        else if (current == ClanRole.MEMBER) newRole = ClanRole.RECRUIT;
        else return;

        clan.getMembers().put(playerId, newRole);
        saveClan(clan);
    }

    public void transfer(Clan clan, UUID newLeader) {
        UUID oldLeader = clan.getLeader();
        clan.getMembers().put(oldLeader, ClanRole.MOD);
        clan.setLeader(newLeader);
        saveClan(clan);
    }

    public void sendInvite(Clan clan, UUID inviter, UUID target) {
        List<ClanInvite> playerInvites = invites.getOrDefault(target, new ArrayList<>());
        playerInvites.add(new ClanInvite(clan.getName(), inviter, target));
        invites.put(target, playerInvites);
    }

    public List<ClanInvite> getInvites(UUID playerId) {
        return invites.getOrDefault(playerId, new ArrayList<>());
    }

    public void removeInvite(UUID playerId, String clanName) {
        List<ClanInvite> playerInvites = invites.get(playerId);
        if (playerInvites != null) {
            playerInvites.removeIf(invite -> invite.getClanName().equalsIgnoreCase(clanName));
        }
    }

    public void setTagColor(Clan clan, String color) {
        clan.setTagColor(color);
        saveClan(clan);
    }

    public List<String> getTagColors() {
        return tagColors;
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }
}
