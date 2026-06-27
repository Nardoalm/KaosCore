package com.knowplugins.ysubz.kaoscore.bukkit.managers.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.account.Account;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.ClanInvite;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import java.util.HashMap;
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

    private boolean isMongo() {
        return Managers.getDatabaseManager() != null && Managers.getDatabaseManager().isMongo();
    }

    private MongoCollection<Document> getCollection(String name) {
        return Managers.getDatabaseManager().getMongoDatabase().getCollection(name);
    }

    private void loadTagColors() {
        tagColors.clear();
        tagColors.addAll(plugin.getConfig().getStringList("clan.tag-colors"));
        if (tagColors.isEmpty()) {
            tagColors.addAll(Arrays.asList(
                    "§0Preto", "§1Azul Escuro", "§2Verde Escuro", "§3Azul Turquesa",
                    "§4Vermelho Escuro", "§5Roxo", "§6Dourado", "§7Cinza",
                    "§8Cinza Escuro", "§9Azul", "§aVerde", "§bAzul Claro",
                    "§cVermelho", "§dRosa", "§eAmarelo", "§fBranco"
            ));
        }
    }

    public void loadClans() {
        if (!hasDatabase()) {
            return;
        }

        cleanupExpiredInvites();

        Map<String, Clan> loadedClans = new HashMap<>();
        Map<UUID, String> loadedPlayerClans = new HashMap<>();

        if (isMongo()) {
            try {
                for (Document doc : getCollection("clans").find()) {
                    String name = doc.getString("name");
                    String tag = doc.getString("tag");
                    String tagColor = doc.getString("tag_color");
                    UUID leader = UUID.fromString(doc.getString("leader"));

                    Clan clan = new Clan(name, tag, leader);
                    clan.setTagColor(tagColor == null || tagColor.trim().isEmpty() ? "§7" : tagColor);
                    clan.setCreationDate(doc.getLong("created"));
                    clan.setWins(doc.getInteger("wins", 0));
                    clan.setLosses(doc.getInteger("losses", 0));
                    clan.setElo(doc.getInteger("elo", 1000));
                    clan.getMembers().clear();

                    Bson memberFilter = Filters.eq("clan_name", name);
                    for (Document mDoc : getCollection("clan_members").find(memberFilter)) {
                        try {
                            UUID uuid = UUID.fromString(mDoc.getString("player_uuid"));
                            ClanRole role = ClanRole.getByName(mDoc.getString("role"));
                            clan.getMembers().put(uuid, role);
                            loadedPlayerClans.put(uuid, name);
                        } catch (Exception ignored) {}
                    }

                    if (!clan.getMembers().containsKey(leader)) {
                        clan.getMembers().put(leader, ClanRole.LEADER);
                        loadedPlayerClans.put(leader, name);
                    }

                    loadedClans.put(name.toLowerCase(Locale.ROOT), clan);
                }

                clans.clear();
                clans.putAll(loadedClans);
                playerClans.clear();
                playerClans.putAll(loadedPlayerClans);
                invites.clear();

                System.out.println("\u001B[32m[KaosCore] " + clans.size() + " clans carregados do MongoDB\u001B[0m");
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao carregar clans do MongoDB: " + e.getMessage() + "\u001B[0m");
            }
            return;
        }

        String selectClans = "SELECT * FROM clans";
        String selectMembers = "SELECT * FROM clan_members WHERE clan_name = ?";

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectClans);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
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
                                try {
                                    UUID uuid = UUID.fromString(memberRs.getString("player_uuid"));
                                    ClanRole role = ClanRole.getByName(memberRs.getString("role"));
                                    clan.getMembers().put(uuid, role);
                                    loadedPlayerClans.put(uuid, name);
                                } catch (Exception memberException) {
                                    if (plugin != null) {
                                        plugin.getLogger().warning("[KaosCore] Membro inválido ignorado ao carregar clan " + name + ": " + memberException.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    if (!clan.getMembers().containsKey(leader)) {
                        clan.getMembers().put(leader, ClanRole.LEADER);
                        loadedPlayerClans.put(leader, name);
                    }

                    loadedClans.put(name.toLowerCase(Locale.ROOT), clan);
                } catch (Exception clanException) {
                    if (plugin != null) {
                        plugin.getLogger().warning("[KaosCore] Clan inválido ignorado ao carregar dados: " + clanException.getMessage());
                    }
                }
            }

            clans.clear();
            clans.putAll(loadedClans);
            playerClans.clear();
            playerClans.putAll(loadedPlayerClans);
            invites.clear();

            System.out.println("\u001B[32m[KaosCore] " + clans.size() + " clans carregados\u001B[0m");
        } catch (Exception e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao carregar clans: " + e.getMessage() + "\u001B[0m");
        }
    }

    public void reload() {
        loadTagColors();
        loadClans();
    }

    public void saveClans() {
        if (!hasDatabase()) {
            return;
        }

        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
    }

    public synchronized void saveClan(Clan clan) {
        if (clan == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Document clanDoc = new Document("name", clan.getName())
                        .append("tag", clan.getTag())
                        .append("tag_color", clan.getTagColor())
                        .append("leader", clan.getLeader().toString())
                        .append("created", clan.getCreationDate())
                        .append("wins", clan.getWins())
                        .append("losses", clan.getLosses())
                        .append("elo", clan.getElo());

                getCollection("clans").replaceOne(Filters.eq("name", clan.getName()), clanDoc, new ReplaceOptions().upsert(true));
                getCollection("clan_members").deleteMany(Filters.eq("clan_name", clan.getName()));

                List<Document> memberDocs = new ArrayList<>();
                for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
                    memberDocs.add(new Document("clan_name", clan.getName())
                            .append("player_uuid", entry.getKey().toString())
                            .append("player_name", resolvePlayerName(entry.getKey()))
                            .append("role", entry.getValue().name())
                            .append("joined", System.currentTimeMillis()));
                }

                if (!memberDocs.isEmpty()) {
                    getCollection("clan_members").insertMany(memberDocs);
                }
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao salvar clan no MongoDB: " + e.getMessage() + "\u001B[0m");
            }
            return;
        }

        String clanSql = "INSERT INTO clans (name, tag, tag_color, leader, created, wins, losses, elo) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE tag = VALUES(tag), tag_color = VALUES(tag_color), leader = VALUES(leader), created = VALUES(created), wins = VALUES(wins), losses = VALUES(losses), elo = VALUES(elo)";

        String clearMembersSql = "DELETE FROM clan_members WHERE clan_name = ?";
        String memberSql = "INSERT INTO clan_members (clan_name, player_uuid, player_name, role, joined) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE clan_name = VALUES(clan_name), role = VALUES(role), player_name = VALUES(player_name)";

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(clanSql);
             PreparedStatement clearMembersStmt = conn.prepareStatement(clearMembersSql);
             PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                stmt.setString(1, clan.getName());
                stmt.setString(2, clan.getTag());
                stmt.setString(3, clan.getTagColor());
                stmt.setString(4, clan.getLeader().toString());
                stmt.setLong(5, clan.getCreationDate());
                stmt.setInt(6, clan.getWins());
                stmt.setInt(7, clan.getLosses());
                stmt.setInt(8, clan.getElo());
                stmt.executeUpdate();

                clearMembersStmt.setString(1, clan.getName());
                clearMembersStmt.executeUpdate();

                for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
                    memberStmt.setString(1, clan.getName());
                    memberStmt.setString(2, entry.getKey().toString());
                    memberStmt.setString(3, resolvePlayerName(entry.getKey()));
                    memberStmt.setString(4, entry.getValue().name());
                    memberStmt.setLong(5, System.currentTimeMillis());
                    memberStmt.addBatch();
                }

                if (!clan.getMembers().isEmpty()) {
                    memberStmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException exception) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    if (plugin != null) {
                        plugin.getLogger().warning("[KaosCore] Falha ao desfazer salvamento do clan " + clan.getName() + ": " + rollbackException.getMessage());
                    }
                }
                throw exception;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
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

        if (hasDatabase()) {
            if (isMongo()) {
                try {
                    getCollection("clans").deleteOne(Filters.eq("name", clan.getName()));
                    getCollection("clan_members").deleteMany(Filters.eq("clan_name", clan.getName()));
                    getCollection("clan_invites").deleteMany(Filters.eq("clan_name", clan.getName()));
                } catch (Exception e) {
                    System.out.println("\u001B[31m[KaosCore] Erro ao deletar clan no MongoDB: " + e.getMessage() + "\u001B[0m");
                }
            } else {
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
        if (playerId == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                getCollection("clan_invites").deleteMany(Filters.eq("target_uuid", playerId.toString()));
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao limpar convites no MongoDB: " + e.getMessage() + "\u001B[0m");
            }
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

    public boolean isTagTaken(String tag, String excludedClanName) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }

        String normalizedTag = tag.trim();
        String excluded = excludedClanName == null ? "" : excludedClanName.trim().toLowerCase(Locale.ROOT);

        for (Clan clan : clans.values()) {
            if (clan == null) {
                continue;
            }

            if (!excluded.isEmpty() && clan.getName() != null && clan.getName().trim().toLowerCase(Locale.ROOT).equals(excluded)) {
                continue;
            }

            if (clan.getTag() != null && clan.getTag().equalsIgnoreCase(normalizedTag)) {
                return true;
            }
        }

        return false;
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
        if (!hasDatabase()) {
            return result;
        }

        if (isMongo()) {
            try {
                Bson filter = Filters.eq("target_uuid", playerId.toString());
                for (Document doc : getCollection("clan_invites").find(filter)) {
                    result.add(new ClanInvite(
                            doc.getString("clan_name"),
                            UUID.fromString(doc.getString("inviter_uuid")),
                            UUID.fromString(doc.getString("target_uuid")),
                            doc.getLong("created_at")
                    ));
                }
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao carregar convites do MongoDB: " + e.getMessage() + "\u001B[0m");
            }
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
        if (!hasDatabase()) {
            return;
        }

        long threshold = System.currentTimeMillis() - ClanInvite.EXPIRATION_MILLIS;

        if (isMongo()) {
            try {
                getCollection("clan_invites").deleteMany(Filters.lt("created_at", threshold));
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao limpar convites expirados do MongoDB: " + e.getMessage() + "\u001B[0m");
            }
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clan_invites WHERE created_at < ?")) {
            stmt.setLong(1, threshold);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KaosCore] Erro ao limpar convites expirados de clan: " + e.getMessage() + "\u001B[0m");
        }
    }

    private void saveInvite(ClanInvite invite) {
        if (invite == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Document doc = new Document("clan_name", invite.getClanName())
                        .append("inviter_uuid", invite.getInviter().toString())
                        .append("target_uuid", invite.getTarget().toString())
                        .append("created_at", invite.getTimestamp());

                Bson filter = Filters.and(
                        Filters.eq("target_uuid", invite.getTarget().toString()),
                        Filters.eq("clan_name", invite.getClanName())
                );
                getCollection("clan_invites").replaceOne(filter, doc, new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao salvar convite no MongoDB: " + e.getMessage() + "\u001B[0m");
            }
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
        if (invite == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Bson filter = Filters.and(
                        Filters.eq("target_uuid", invite.getTarget().toString()),
                        Filters.eq("clan_name", invite.getClanName())
                );
                getCollection("clan_invites").deleteOne(filter);
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao remover convite do MongoDB: " + e.getMessage() + "\u001B[0m");
            }
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
        if (playerId == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                getCollection("clan_members").deleteOne(Filters.eq("player_uuid", playerId.toString()));
            } catch (Exception e) {
                System.out.println("\u001B[31m[KaosCore] Erro ao remover membro do MongoDB: " + e.getMessage() + "\u001B[0m");
            }
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

        if (plugin != null && plugin.getAccountController() != null) {
            Account account = plugin.getAccountController().get(playerId);
            if (account != null && account.getName() != null && !account.getName().trim().isEmpty()) {
                return account.getName();
            }
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

    private boolean hasDatabase() {
        return Managers.getDatabaseManager() != null;
    }
}