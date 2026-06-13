package com.knowplugins.ysubz.kaoscore.bungee.services;

import com.knowplugins.ysubz.kaoscore.bungee.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.model.Clan;
import com.knowplugins.ysubz.kaoscore.bungee.model.ClanInvite;
import com.knowplugins.ysubz.kaoscore.bungee.model.ClanRole;
import com.knowplugins.ysubz.kaoscore.bungee.repository.BungeePlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bungee.util.Texts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BungeeClanService {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeePlayerAddressRepository addresses;
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerClans = new ConcurrentHashMap<>();
    private final List<String> tagColors = new ArrayList<>();

    public BungeeClanService(BungeeKaosCore plugin, BungeeDatabase database, BungeePlayerAddressRepository addresses) {
        this.plugin = plugin;
        this.database = database;
        this.addresses = addresses;
        loadTagColors();
        reload();
    }

    public void reload() {
        cleanupExpiredInvites();
        Map<String, Clan> loadedClans = new HashMap<>();
        Map<UUID, String> loadedPlayerClans = new HashMap<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM clans");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String tag = resultSet.getString("tag");
                UUID leader = UUID.fromString(resultSet.getString("leader"));
                Clan clan = new Clan(name, tag, leader);
                clan.setTagColor(resultSet.getString("tag_color"));
                clan.setCreationDate(resultSet.getLong("created"));
                clan.setWins(resultSet.getInt("wins"));
                clan.setLosses(resultSet.getInt("losses"));
                clan.setElo(resultSet.getInt("elo"));
                clan.getMembers().clear();

                try (PreparedStatement memberStatement = connection.prepareStatement("SELECT * FROM clan_members WHERE clan_name = ?")) {
                    memberStatement.setString(1, name);
                    try (ResultSet memberSet = memberStatement.executeQuery()) {
                        while (memberSet.next()) {
                            UUID memberId = UUID.fromString(memberSet.getString("player_uuid"));
                            ClanRole role = ClanRole.getByName(memberSet.getString("role"));
                            clan.getMembers().put(memberId, role);
                            loadedPlayerClans.put(memberId, name);
                        }
                    }
                }

                if (!clan.getMembers().containsKey(leader)) {
                    clan.getMembers().put(leader, ClanRole.LEADER);
                    loadedPlayerClans.put(leader, name);
                }

                loadedClans.put(Texts.normalize(name), clan);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao carregar clans: " + exception.getMessage());
        }

        clans.clear();
        clans.putAll(loadedClans);
        playerClans.clear();
        playerClans.putAll(loadedPlayerClans);
    }

    public Collection<Clan> getAllClans() {
        return new ArrayList<>(clans.values());
    }

    public Clan getClan(String nameOrTag) {
        if (nameOrTag == null) {
            return null;
        }
        Clan byName = clans.get(Texts.normalize(nameOrTag));
        if (byName != null) {
            return byName;
        }
        for (Clan clan : clans.values()) {
            if (clan.getTag().equalsIgnoreCase(nameOrTag)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getPlayerClan(UUID playerId) {
        String clanName = playerClans.get(playerId);
        return clanName == null ? null : clans.get(Texts.normalize(clanName));
    }

    public boolean isInClan(UUID playerId) {
        return playerId != null && playerClans.containsKey(playerId);
    }

    public boolean createClan(String name, String tag, UUID leaderId, String leaderName) {
        reload();
        if (leaderId == null || Texts.normalize(name).isEmpty() || Texts.normalize(tag).isEmpty() || clans.containsKey(Texts.normalize(name)) || playerClans.containsKey(leaderId)) {
            return false;
        }
        for (Clan existing : clans.values()) {
            if (existing.getTag().equalsIgnoreCase(tag)) {
                return false;
            }
        }
        Clan clan = new Clan(name, tag, leaderId);
        clans.put(Texts.normalize(name), clan);
        playerClans.put(leaderId, name);
        saveClan(clan);
        return true;
    }

    public boolean deleteClan(String name, UUID deleterId) {
        reload();
        Clan clan = getClan(name);
        if (clan == null || deleterId == null || !clan.isLeader(deleterId)) {
            return false;
        }
        for (UUID member : new ArrayList<>(clan.getMembers().keySet())) {
            playerClans.remove(member);
        }
        clans.remove(Texts.normalize(clan.getName()));
        try (Connection connection = database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM clans WHERE name = ?")) {
                statement.setString(1, clan.getName());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM clan_members WHERE clan_name = ?")) {
                statement.setString(1, clan.getName());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM clan_invites WHERE clan_name = ?")) {
                statement.setString(1, clan.getName());
                statement.executeUpdate();
            }
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao deletar clan: " + exception.getMessage());
            return false;
        }
    }

    public void saveClan(Clan clan) {
        if (clan == null) {
            return;
        }
        String clanSql = "INSERT INTO clans (name, tag, tag_color, leader, created, wins, losses, elo) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE tag = VALUES(tag), tag_color = VALUES(tag_color), leader = VALUES(leader), created = VALUES(created), wins = VALUES(wins), losses = VALUES(losses), elo = VALUES(elo)";
        String clearMembersSql = "DELETE FROM clan_members WHERE clan_name = ?";
        String memberSql = "INSERT INTO clan_members (clan_name, player_uuid, player_name, role, joined) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE clan_name = VALUES(clan_name), role = VALUES(role), player_name = VALUES(player_name)";

        try (Connection connection = database.getConnection();
             PreparedStatement clanStatement = connection.prepareStatement(clanSql);
             PreparedStatement clearStatement = connection.prepareStatement(clearMembersSql);
             PreparedStatement memberStatement = connection.prepareStatement(memberSql)) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                clanStatement.setString(1, clan.getName());
                clanStatement.setString(2, clan.getTag());
                clanStatement.setString(3, clan.getTagColor());
                clanStatement.setString(4, clan.getLeader().toString());
                clanStatement.setLong(5, clan.getCreationDate());
                clanStatement.setInt(6, clan.getWins());
                clanStatement.setInt(7, clan.getLosses());
                clanStatement.setInt(8, clan.getElo());
                clanStatement.executeUpdate();

                clearStatement.setString(1, clan.getName());
                clearStatement.executeUpdate();

                for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
                    memberStatement.setString(1, clan.getName());
                    memberStatement.setString(2, entry.getKey().toString());
                    memberStatement.setString(3, addresses.findLatestName(entry.getKey()));
                    memberStatement.setString(4, entry.getValue().name());
                    memberStatement.setLong(5, System.currentTimeMillis());
                    memberStatement.addBatch();
                }
                if (!clan.getMembers().isEmpty()) {
                    memberStatement.executeBatch();
                }
                connection.commit();
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao salvar clan: " + exception.getMessage());
        }
        reload();
    }

    public void addMember(Clan clan, UUID playerId, ClanRole role) {
        if (clan == null || playerId == null || role == null) {
            return;
        }
        clan.addMember(playerId, role);
        playerClans.put(playerId, clan.getName());
        saveClan(clan);
        clearInvites(playerId);
    }

    public void removeMember(Clan clan, UUID playerId) {
        if (clan == null || playerId == null) {
            return;
        }
        clan.removeMember(playerId);
        playerClans.remove(playerId);
        deleteMemberRecord(playerId);
        saveClan(clan);
    }

    public void promote(Clan clan, UUID playerId) {
        ClanRole current = clan == null ? null : clan.getRole(playerId);
        if (current == ClanRole.RECRUIT) {
            clan.getMembers().put(playerId, ClanRole.MEMBER);
            saveClan(clan);
        } else if (current == ClanRole.MEMBER) {
            clan.getMembers().put(playerId, ClanRole.MOD);
            saveClan(clan);
        }
    }

    public void demote(Clan clan, UUID playerId) {
        ClanRole current = clan == null ? null : clan.getRole(playerId);
        if (current == ClanRole.MOD) {
            clan.getMembers().put(playerId, ClanRole.MEMBER);
            saveClan(clan);
        } else if (current == ClanRole.MEMBER) {
            clan.getMembers().put(playerId, ClanRole.RECRUIT);
            saveClan(clan);
        }
    }

    public void transfer(Clan clan, UUID newLeader) {
        if (clan == null || newLeader == null) {
            return;
        }
        UUID oldLeader = clan.getLeader();
        clan.getMembers().put(oldLeader, ClanRole.MOD);
        clan.setLeader(newLeader);
        saveClan(clan);
    }

    public void sendInvite(Clan clan, UUID inviter, UUID target) {
        if (clan == null || inviter == null || target == null) {
            return;
        }
        removeInvite(target, clan.getName());
        ClanInvite invite = new ClanInvite(clan.getName(), inviter, target);
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO clan_invites (clan_name, inviter_uuid, target_uuid, created_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE inviter_uuid = VALUES(inviter_uuid), created_at = VALUES(created_at)")) {
            statement.setString(1, invite.getClanName());
            statement.setString(2, invite.getInviter().toString());
            statement.setString(3, invite.getTarget().toString());
            statement.setLong(4, invite.getTimestamp());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao salvar convite de clan: " + exception.getMessage());
        }
    }

    public List<ClanInvite> getInvites(UUID playerId) {
        List<ClanInvite> result = new ArrayList<>();
        if (playerId == null) {
            return result;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT clan_name, inviter_uuid, target_uuid, created_at FROM clan_invites WHERE target_uuid = ? ORDER BY created_at DESC")) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ClanInvite invite = new ClanInvite(
                            resultSet.getString("clan_name"),
                            UUID.fromString(resultSet.getString("inviter_uuid")),
                            UUID.fromString(resultSet.getString("target_uuid")),
                            resultSet.getLong("created_at")
                    );
                    if (!invite.isExpired()) {
                        result.add(invite);
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao carregar convites de clan: " + exception.getMessage());
        }
        return result;
    }

    public boolean hasInvite(UUID playerId, String clanNameOrTag) {
        for (ClanInvite invite : getInvites(playerId)) {
            if (matchesInvite(invite, clanNameOrTag)) {
                return true;
            }
        }
        return false;
    }

    public void removeInvite(UUID playerId, String clanNameOrTag) {
        if (playerId == null || clanNameOrTag == null) {
            return;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM clan_invites WHERE target_uuid = ? AND clan_name = ?")) {
            Clan clan = getClan(clanNameOrTag);
            statement.setString(1, playerId.toString());
            statement.setString(2, clan == null ? clanNameOrTag : clan.getName());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao remover convite de clan: " + exception.getMessage());
        }
    }

    public void clearInvites(UUID playerId) {
        if (playerId == null) {
            return;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM clan_invites WHERE target_uuid = ?")) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao limpar convites de clan: " + exception.getMessage());
        }
    }

    public void setTagColor(Clan clan, String color) {
        if (clan != null) {
            clan.setTagColor(color);
            saveClan(clan);
        }
    }

    public List<String> getTagColors() {
        return Collections.unmodifiableList(tagColors);
    }

    public boolean isTagTaken(String tag, String excludedClanName) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        String excluded = Texts.normalize(excludedClanName);
        for (Clan clan : clans.values()) {
            if (!excluded.isEmpty() && Texts.normalize(clan.getName()).equals(excluded)) {
                continue;
            }
            if (clan.getTag().equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesInvite(ClanInvite invite, String clanNameOrTag) {
        if (invite == null || clanNameOrTag == null) {
            return false;
        }
        if (invite.getClanName().equalsIgnoreCase(clanNameOrTag)) {
            return true;
        }
        Clan clan = getClan(invite.getClanName());
        return clan != null && clan.getTag().equalsIgnoreCase(clanNameOrTag);
    }

    private void deleteMemberRecord(UUID playerId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM clan_members WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao remover membro de clan: " + exception.getMessage());
        }
    }

    private void cleanupExpiredInvites() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM clan_invites WHERE created_at < ?")) {
            statement.setLong(1, System.currentTimeMillis() - ClanInvite.EXPIRATION_MILLIS);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao limpar convites expirados: " + exception.getMessage());
        }
    }

    private void loadTagColors() {
        Collections.addAll(tagColors,
                "§0Preto", "§1Azul Escuro", "§2Verde Escuro", "§3Ciano",
                "§4Vermelho Escuro", "§5Roxo", "§6Dourado", "§7Cinza",
                "§8Cinza Escuro", "§9Azul", "§aVerde", "§bAzul Claro",
                "§cVermelho", "§dRosa", "§eAmarelo", "§fBranco");
    }
    
    public BungeeKaosCore getPlugin() {
        return plugin;
    }
}
