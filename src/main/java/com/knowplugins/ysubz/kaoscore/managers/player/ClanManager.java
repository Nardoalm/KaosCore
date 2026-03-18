package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.clan.Clan;
import com.knowplugins.ysubz.kaoscore.clan.ClanInvite;
import com.knowplugins.ysubz.kaoscore.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClanManager {

    private final KaosCore plugin;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerClans;
    private final Map<UUID, List<ClanInvite>> invites;
    private final List<String> tagColors;

    public ClanManager(KaosCore plugin) {
        this.plugin = plugin;
        this.clans = new ConcurrentHashMap<>();
        this.playerClans = new ConcurrentHashMap<>();
        this.invites = new ConcurrentHashMap<>();
        this.tagColors = new ArrayList<>();
        loadTagColors();
    }

    private void loadTagColors() {
        tagColors.clear();
        tagColors.addAll(plugin.getConfig().getStringList("clan.tag-colors"));
        if (tagColors.isEmpty()) {
            tagColors.addAll(Arrays.asList(
                    "§0Preto",
                    "§1Azul Escuro",
                    "§2Verde Escuro",
                    "§3Azul Turquesa",
                    "§4Vermelho Escuro",
                    "§5Roxo",
                    "§6Dourado",
                    "§7Cinza",
                    "§8Cinza Escuro",
                    "§9Azul",
                    "§aVerde",
                    "§bAzul Claro",
                    "§cVermelho",
                    "§dRosa",
                    "§eAmarelo",
                    "§fBranco"
            ));
        }
    }

    public void loadClans() {
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        clans.clear();
        playerClans.clear();
        invites.clear();
        cleanupExpiredInvites();

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
                clan.setTagColor(tagColor == null || tagColor.trim().isEmpty() ? "§7" : tagColor);
                clan.setCreationDate(rs.getLong("created"));
                clan.setWins(rs.getInt("wins"));
                clan.setLosses(rs.getInt("losses"));
                clan.setElo(rs.getInt("elo"));
                clan.getMembers().clear();

                try (PreparedStatement memberStmt = conn.prepareStatement(selectMembers)) {
                    memberStmt.setString(1, name);
                    try (ResultSet memberRs = memberStmt.executeQuery()) {
                        while (memberRs.next()) {
                            UUID uuid = UUID.fromString(memberRs.getString("player_uuid"));
                            ClanRole role = ClanRole.getByName(memberRs.getString("role"));
                            clan.getMembers().put(uuid, role);
                            playerClans.put(uuid, name);
                        }
                    }
                }

                if (!clan.getMembers().containsKey(leader)) {
                    clan.getMembers().put(leader, ClanRole.LEADER);
                    playerClans.put(leader, name);
                }

                clans.put(name.toLowerCase(Locale.ROOT), clan);
            }

            System.out.println("\u001B[32m[KaosCore] " + clans.size() + " clans carregados\u001B[0m");
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao carregar clans: " + e.getMessage() + "\u001B[0m");
        }
    }

    public void saveClans() {
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
    }

    public synchronized void saveClan(Clan clan) {
        if (clan == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        String clanSql = "INSERT INTO clans (name, tag, tag_color, leader, created, wins, losses, elo) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE tag = VALUES(tag), tag_color = VALUES(tag_color), leader = VALUES(leader), created = VALUES(created), wins = VALUES(wins), losses = VALUES(losses), elo = VALUES(elo)";

        String memberSql = "INSERT INTO clan_members (clan_name, player_uuid, player_name, role, joined) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE clan_name = VALUES(clan_name), role = VALUES(role), player_name = VALUES(player_name)";

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(clanSql);
             PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {

            stmt.setString(1, clan.getName());
            stmt.setString(2, clan.getTag());
            stmt.setString(3, clan.getTagColor());
            stmt.setString(4, clan.getLeader().toString());
            stmt.setLong(5, clan.getCreationDate());
            stmt.setInt(6, clan.getWins());
            stmt.setInt(7, clan.getLosses());
            stmt.setInt(8, clan.getElo());
            stmt.executeUpdate();

            for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
                memberStmt.setString(1, clan.getName());
                memberStmt.setString(2, entry.getKey().toString());
                memberStmt.setString(3, resolvePlayerName(entry.getKey()));
                memberStmt.setString(4, entry.getValue().name());
                memberStmt.setLong(5, System.currentTimeMillis());
                memberStmt.addBatch();
            }
            memberStmt.executeBatch();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao salvar clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    public boolean createClan(String name, String tag, Player leader) {
        if (leader == null || clans.containsKey(name.toLowerCase(Locale.ROOT)) || playerClans.containsKey(leader.getUniqueId())) {
            return false;
        }

        for (Clan existing : clans.values()) {
            if (existing.getTag().equalsIgnoreCase(tag)) {
                return false;
            }
        }

        Clan clan = new Clan(name, tag, leader.getUniqueId());
        clans.put(name.toLowerCase(Locale.ROOT), clan);
        playerClans.put(leader.getUniqueId(), name);
        saveClan(clan);
        refreshPlayerTag(leader.getUniqueId());

        System.out.println("\u001B[32m[KaosCore] Clan criado: " + name + " [" + tag + "] por " + leader.getName() + "\u001B[0m");
        return true;
    }

    public boolean deleteClan(String name, Player deleter) {
        Clan clan = getClan(name);
        if (clan == null || deleter == null || !clan.isLeader(deleter.getUniqueId())) {
            return false;
        }

        for (UUID member : new ArrayList<>(clan.getMembers().keySet())) {
            playerClans.remove(member);
            invites.remove(member);
            refreshPlayerTag(member);
        }

        clans.remove(clan.getName().toLowerCase(Locale.ROOT));

        if (Managers.getDatabaseManager() != null && Managers.getDatabaseManager().isConnected()) {
            try (Connection conn = Managers.getDatabaseManager().getConnection();
                 PreparedStatement deleteClanStmt = conn.prepareStatement("DELETE FROM clans WHERE name = ?");
                 PreparedStatement deleteMembersStmt = conn.prepareStatement("DELETE FROM clan_members WHERE clan_name = ?");
                 PreparedStatement deleteInvitesStmt = conn.prepareStatement("DELETE FROM clan_invites WHERE clan_name = ?")) {
                deleteClanStmt.setString(1, clan.getName());
                deleteMembersStmt.setString(1, clan.getName());
                deleteInvitesStmt.setString(1, clan.getName());
                deleteClanStmt.executeUpdate();
                deleteMembersStmt.executeUpdate();
                deleteInvitesStmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao deletar clan: " + e.getMessage() + "\u001B[0m");
            }
        }

        System.out.println("\u001B[33m[KaosCore] Clan deletado: " + clan.getName() + " por " + deleter.getName() + "\u001B[0m");
        return true;
    }

    public Clan getClan(String nameOrTag) {
        if (nameOrTag == null) {
            return null;
        }

        Clan byName = clans.get(nameOrTag.toLowerCase(Locale.ROOT));
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
        if (clanName == null) {
            return null;
        }
        return clans.get(clanName.toLowerCase(Locale.ROOT));
    }

    public Clan getClan(Player player) {
        return player == null ? null : getPlayerClan(player.getUniqueId());
    }

    public boolean isInClan(UUID playerId) {
        return playerId != null && playerClans.containsKey(playerId);
    }

    public void addMember(Clan clan, UUID playerId, ClanRole role) {
        if (clan == null || playerId == null || role == null) {
            return;
        }

        clan.addMember(playerId, role);
        playerClans.put(playerId, clan.getName());
        saveClan(clan);
        clearInvites(playerId);
        refreshPlayerTag(playerId);
    }

    public void removeMember(Clan clan, UUID playerId) {
        if (clan == null || playerId == null) {
            return;
        }

        clan.removeMember(playerId);
        playerClans.remove(playerId);
        deleteMemberRecord(playerId);
        saveClan(clan);
        refreshPlayerTag(playerId);
    }

    public void promote(Clan clan, UUID playerId) {
        ClanRole current = clan == null ? null : clan.getRole(playerId);
        if (current == null) {
            return;
        }

        ClanRole newRole;
        if (current == ClanRole.RECRUIT) {
            newRole = ClanRole.MEMBER;
        } else if (current == ClanRole.MEMBER) {
            newRole = ClanRole.MOD;
        } else {
            return;
        }

        clan.getMembers().put(playerId, newRole);
        saveClan(clan);
    }

    public void demote(Clan clan, UUID playerId) {
        ClanRole current = clan == null ? null : clan.getRole(playerId);
        if (current == null) {
            return;
        }

        ClanRole newRole;
        if (current == ClanRole.MOD) {
            newRole = ClanRole.MEMBER;
        } else if (current == ClanRole.MEMBER) {
            newRole = ClanRole.RECRUIT;
        } else {
            return;
        }

        clan.getMembers().put(playerId, newRole);
        saveClan(clan);
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

        List<ClanInvite> playerInvites = loadInvites(target, false);
        playerInvites.removeIf(invite -> matchesInvite(invite, clan.getName()));

        ClanInvite invite = new ClanInvite(clan.getName(), inviter, target);
        playerInvites.add(invite);
        invites.put(target, playerInvites);
        saveInvite(invite);
    }

    public boolean hasInvite(UUID playerId, String clanNameOrTag) {
        for (ClanInvite invite : getInvites(playerId)) {
            if (matchesInvite(invite, clanNameOrTag)) {
                return true;
            }
        }
        return false;
    }

    public List<ClanInvite> getInvites(UUID playerId) {
        return Collections.unmodifiableList(new ArrayList<>(loadInvites(playerId, false)));
    }

    public void removeInvite(UUID playerId, String clanNameOrTag) {
        List<ClanInvite> playerInvites = loadInvites(playerId, false);
        List<ClanInvite> removed = new ArrayList<>();

        for (ClanInvite invite : new ArrayList<>(playerInvites)) {
            if (matchesInvite(invite, clanNameOrTag)) {
                playerInvites.remove(invite);
                removed.add(invite);
            }
        }

        invites.put(playerId, playerInvites);
        for (ClanInvite invite : removed) {
            deleteInvite(invite);
        }
    }

    public void clearInvites(UUID playerId) {
        invites.remove(playerId);
        if (playerId == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clan_invites WHERE target_uuid = ?")) {
            stmt.setString(1, playerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao limpar convites de clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    public void notifyPendingInvites(Player player) {
        sendInviteList(player, false, true);
    }

    public boolean sendInviteList(Player player, boolean announceEmpty, boolean fromLogin) {
        if (player == null || isInClan(player.getUniqueId())) {
            return false;
        }

        List<ClanInvite> playerInvites = getInvites(player.getUniqueId());
        if (playerInvites.isEmpty()) {
            if (announceEmpty) {
                player.sendMessage("§cVocê não tem convites pendentes.");
            }
            return false;
        }

        if (fromLogin) {
            player.sendMessage("§6Você possui §e" + playerInvites.size() + " §6convite(s) de clan pendente(s):");
        } else {
            player.sendMessage("§6§m+-------------------§6[ §eConvites §6]§m-------------------+");
        }

        for (ClanInvite invite : playerInvites) {
            player.spigot().sendMessage(buildInviteComponent(invite));
        }

        if (!fromLogin) {
            player.sendMessage("§6§m----------------------------------------------------------+");
        }
        return true;
    }

    public void sendInviteNotification(Player target, Clan clan, UUID inviterId) {
        if (target == null || clan == null) {
            return;
        }

        String inviterName = resolvePlayerName(inviterId);
        target.sendMessage("§6Você foi convidado para o clan §f" + clan.getName() + " §6" + clan.getFormattedTag());
        target.sendMessage("§7Convite enviado por: §f" + inviterName);
        target.spigot().sendMessage(buildInviteComponent(new ClanInvite(clan.getName(), inviterId, target.getUniqueId())));
    }

    public void setTagColor(Clan clan, String color) {
        if (clan == null || color == null) {
            return;
        }
        clan.setTagColor(color);
        saveClan(clan);

        for (UUID memberId : new ArrayList<>(clan.getMembers().keySet())) {
            refreshPlayerTag(memberId);
        }
    }

    public List<String> getTagColors() {
        return Collections.unmodifiableList(tagColors);
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }

    private List<ClanInvite> loadInvites(UUID playerId, boolean forceReload) {
        if (playerId == null) {
            return new ArrayList<>();
        }

        if (forceReload || !invites.containsKey(playerId)) {
            invites.put(playerId, loadInvitesFromDatabase(playerId));
        }

        List<ClanInvite> playerInvites = invites.get(playerId);
        if (playerInvites == null) {
            playerInvites = new ArrayList<>();
            invites.put(playerId, playerInvites);
        }

        pruneExpiredInvites(playerId, playerInvites);
        return playerInvites;
    }

    private List<ClanInvite> loadInvitesFromDatabase(UUID playerId) {
        List<ClanInvite> result = new ArrayList<>();
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return result;
        }

        String sql = "SELECT clan_name, inviter_uuid, target_uuid, created_at FROM clan_invites WHERE target_uuid = ? ORDER BY created_at DESC";
        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new ClanInvite(
                            rs.getString("clan_name"),
                            UUID.fromString(rs.getString("inviter_uuid")),
                            UUID.fromString(rs.getString("target_uuid")),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao carregar convites de clan: " + e.getMessage() + "\u001B[0m");
        }

        return result;
    }

    private void pruneExpiredInvites(UUID playerId, List<ClanInvite> playerInvites) {
        if (playerId == null || playerInvites == null || playerInvites.isEmpty()) {
            return;
        }

        List<ClanInvite> expired = new ArrayList<>();
        for (ClanInvite invite : new ArrayList<>(playerInvites)) {
            if (invite.isExpired()) {
                playerInvites.remove(invite);
                expired.add(invite);
            }
        }

        invites.put(playerId, playerInvites);
        for (ClanInvite invite : expired) {
            deleteInvite(invite);
        }
    }

    private void cleanupExpiredInvites() {
        if (Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clan_invites WHERE created_at < ?")) {
            stmt.setLong(1, System.currentTimeMillis() - ClanInvite.EXPIRATION_MILLIS);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao limpar convites expirados de clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    private void saveInvite(ClanInvite invite) {
        if (invite == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        String sql = "INSERT INTO clan_invites (clan_name, inviter_uuid, target_uuid, created_at) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE inviter_uuid = VALUES(inviter_uuid), created_at = VALUES(created_at)";
        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invite.getClanName());
            stmt.setString(2, invite.getInviter().toString());
            stmt.setString(3, invite.getTarget().toString());
            stmt.setLong(4, invite.getTimestamp());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao salvar convite de clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    private void deleteInvite(ClanInvite invite) {
        if (invite == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clan_invites WHERE target_uuid = ? AND clan_name = ?")) {
            stmt.setString(1, invite.getTarget().toString());
            stmt.setString(2, invite.getClanName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao remover convite de clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    private void deleteMemberRecord(UUID playerId) {
        if (playerId == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clan_members WHERE player_uuid = ?")) {
            stmt.setString(1, playerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao remover membro do clan: " + e.getMessage() + "\u001B[0m");
        }
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

    private String resolvePlayerName(UUID playerId) {
        if (playerId == null) {
            return "Desconhecido";
        }

        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        return offline != null && offline.getName() != null ? offline.getName() : "Desconhecido";
    }

    private TextComponent buildInviteComponent(ClanInvite invite) {
        TextComponent line = new TextComponent(" §7- §f" + invite.getClanName() + " ");

        TextComponent accept = new TextComponent("§a[ACEITAR]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan aceitar " + invite.getClanName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para aceitar o convite").create()));

        TextComponent space = new TextComponent(" ");

        TextComponent deny = new TextComponent("§c[RECUSAR]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan recusar " + invite.getClanName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClique para recusar o convite").create()));

        line.addExtra(accept);
        line.addExtra(space);
        line.addExtra(deny);
        return line;
    }

    private void refreshPlayerTag(UUID playerId) {
        if (playerId == null || plugin == null || plugin.getTagManager() == null) {
            return;
        }

        Player online = Bukkit.getPlayer(playerId);
        if (online != null && online.isOnline()) {
            plugin.getTagManager().refreshTag(online);
        }
    }
}
