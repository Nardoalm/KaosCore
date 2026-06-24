package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.account.Account;
import com.knowplugins.ysubz.kaoscore.friend.FriendRequest;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.utils.tag.Tag;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
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
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager {

    public enum RequestResult {
        SENT,
        AUTO_ACCEPTED,
        ALREADY_FRIENDS,
        ALREADY_REQUESTED,
        LIMIT_REACHED,
        SELF,
        INVALID
    }

    private static final int MAX_FRIENDS = 100;

    private final KaosCore plugin;
    private final Map<UUID, Set<UUID>> friends;
    private final Map<UUID, List<FriendRequest>> requests;

    public FriendManager(KaosCore plugin) {
        this.plugin = plugin;
        this.friends = new ConcurrentHashMap<>();
        this.requests = new ConcurrentHashMap<>();
    }

    private boolean isMongo() {
        return Managers.getDatabaseManager() != null && Managers.getDatabaseManager().isMongo();
    }

    private MongoCollection<Document> getCollection(String name) {
        return Managers.getDatabaseManager().getMongoDatabase().getCollection(name);
    }

    public void loadFriends() {
        friends.clear();
        requests.clear();

        if (!hasDatabase()) {
            return;
        }

        cleanupExpiredRequests();

        if (isMongo()) {
            try {
                for (Document doc : getCollection("friends").find()) {
                    UUID playerId = UUID.fromString(doc.getString("player_uuid"));
                    UUID friendId = UUID.fromString(doc.getString("friend_uuid"));
                    friends.computeIfAbsent(playerId, key -> ConcurrentHashMap.newKeySet()).add(friendId);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar amigos do MongoDB: " + e.getMessage());
            }
            return;
        }

        String sql = "SELECT player_uuid, friend_uuid FROM friends";
        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    UUID friendId = UUID.fromString(rs.getString("friend_uuid"));
                    friends.computeIfAbsent(playerId, key -> ConcurrentHashMap.newKeySet()).add(friendId);
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao carregar amigos: " + e.getMessage());
        }
    }

    public RequestResult createRequest(Player requester, Player target) {
        return createRequest(requester == null ? null : new Account(requester.getUniqueId(), requester.getName()),
                target == null ? null : new Account(target.getUniqueId(), target.getName()));
    }

    public RequestResult createRequest(Account requester, Account target) {
        if (requester == null || target == null) {
            return RequestResult.INVALID;
        }
        if (requester.getId().equals(target.getId())) {
            return RequestResult.SELF;
        }
        if (areFriends(requester.getId(), target.getId())) {
            return RequestResult.ALREADY_FRIENDS;
        }
        if (!canAddMoreFriends(requester.getId()) || !canAddMoreFriends(target.getId())) {
            return RequestResult.LIMIT_REACHED;
        }
        if (hasPendingRequest(requester.getId(), target.getId())) {
            return RequestResult.ALREADY_REQUESTED;
        }
        if (hasPendingRequest(target.getId(), requester.getId())) {
            return acceptRequest(requester.getId(), target.getId())
                    ? RequestResult.AUTO_ACCEPTED
                    : RequestResult.LIMIT_REACHED;
        }

        FriendRequest request = new FriendRequest(
                requester.getId(),
                requester.getName(),
                target.getId()
        );

        List<FriendRequest> targetRequests = loadRequests(target.getId(), false);
        targetRequests.add(request);
        requests.put(target.getId(), targetRequests);
        saveRequest(request);
        return RequestResult.SENT;
    }

    public boolean acceptRequest(UUID targetId, UUID requesterId) {
        if (targetId == null || requesterId == null) {
            return false;
        }
        if (!hasPendingRequest(requesterId, targetId)) {
            return false;
        }
        if (areFriends(requesterId, targetId)) {
            removeRequest(targetId, requesterId);
            return true;
        }
        if (!canAddMoreFriends(requesterId) || !canAddMoreFriends(targetId)) {
            return false;
        }

        addFriendPair(requesterId, targetId, System.currentTimeMillis());
        removeRequest(targetId, requesterId);
        removeRequest(requesterId, targetId);
        return true;
    }

    public void denyRequest(UUID targetId, UUID requesterId) {
        removeRequest(targetId, requesterId);
    }

    public void removeFriend(UUID playerId, UUID friendId) {
        if (playerId == null || friendId == null) {
            return;
        }

        Set<UUID> playerFriends = friends.get(playerId);
        if (playerFriends != null) {
            playerFriends.remove(friendId);
        }

        Set<UUID> otherFriends = friends.get(friendId);
        if (otherFriends != null) {
            otherFriends.remove(playerId);
        }

        deleteFriendPair(playerId, friendId);
    }

    public boolean areFriends(UUID playerId, UUID friendId) {
        if (playerId == null || friendId == null) {
            return false;
        }
        return friends.getOrDefault(playerId, Collections.emptySet()).contains(friendId);
    }

    public boolean canAddMoreFriends(UUID playerId) {
        return getFriendCount(playerId) < MAX_FRIENDS;
    }

    public int getFriendCount(UUID playerId) {
        return getFriends(playerId).size();
    }

    public int getOnlineFriendCount(UUID playerId) {
        int online = 0;
        for (UUID friendId : getFriends(playerId)) {
            Player friend = Bukkit.getPlayer(friendId);
            if (friend != null && friend.isOnline() && canSeeFriend(playerId, friend)) {
                online++;
            }
        }
        return online;
    }

    public Set<UUID> getFriends(UUID playerId) {
        if (playerId == null) {
            return Collections.emptySet();
        }
        Set<UUID> set = friends.get(playerId);
        if (set == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(set));
    }

    public List<FriendRequest> getRequests(UUID targetId) {
        return Collections.unmodifiableList(new ArrayList<>(loadRequests(targetId, false)));
    }

    public boolean hasPendingRequest(UUID requesterId, UUID targetId) {
        if (requesterId == null || targetId == null) {
            return false;
        }
        for (FriendRequest request : loadRequests(targetId, false)) {
            if (request.getRequesterId().equals(requesterId)) {
                return true;
            }
        }
        return false;
    }

    public FriendRequest findRequest(UUID targetId, String identifier) {
        if (targetId == null || identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String normalized = normalize(identifier);
        for (FriendRequest request : getRequests(targetId)) {
            if (request.getRequesterId().toString().equalsIgnoreCase(normalized)) {
                return request;
            }

            if (normalize(request.getRequesterName()).equals(normalized)) {
                return request;
            }

            Player online = Bukkit.getPlayer(request.getRequesterId());
            if (online != null) {
                if (normalize(online.getName()).equals(normalized)) {
                    return request;
                }
                if (normalize(ChatColor.stripColor(online.getDisplayName())).equals(normalized)) {
                    return request;
                }
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(request.getRequesterId());
            if (offline != null && offline.getName() != null && normalize(offline.getName()).equals(normalized)) {
                return request;
            }
        }
        return null;
    }

    public FriendRequest getRequest(UUID targetId, UUID requesterId) {
        if (targetId == null || requesterId == null) {
            return null;
        }
        for (FriendRequest request : getRequests(targetId)) {
            if (request.getRequesterId().equals(requesterId)) {
                return request;
            }
        }
        return null;
    }

    public UUID findFriend(UUID ownerId, String identifier) {
        if (ownerId == null || identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String normalized = normalize(identifier);
        for (UUID friendId : getFriends(ownerId)) {
            if (friendId.toString().equalsIgnoreCase(normalized)) {
                return friendId;
            }

            Player online = Bukkit.getPlayer(friendId);
            if (online != null) {
                if (normalize(online.getName()).equals(normalized)) {
                    return friendId;
                }
                if (normalize(ChatColor.stripColor(online.getDisplayName())).equals(normalized)) {
                    return friendId;
                }
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(friendId);
            if (offline != null && offline.getName() != null && normalize(offline.getName()).equals(normalized)) {
                return friendId;
            }
        }

        return null;
    }

    public void notifyPendingRequests(Player player) {
        sendRequestList(player, false, true);
    }

    public boolean sendRequestList(Player player, boolean announceEmpty, boolean fromLogin) {
        if (player == null) {
            return false;
        }

        List<FriendRequest> playerRequests = getRequests(player.getUniqueId());
        if (playerRequests.isEmpty()) {
            if (announceEmpty) {
                player.sendMessage("§cYou do not have any pending friend requests.");
            }
            return false;
        }

        if (fromLogin) {
            player.sendMessage("§6You have §e" + playerRequests.size() + " §6pending friend request(s):");
        } else {
            player.sendMessage("§6§m+-------------------§6[ §eFriend Requests §6]§m-------------------+");
        }

        for (FriendRequest request : playerRequests) {
            player.spigot().sendMessage(buildRequestComponent(request));
        }

        if (!fromLogin) {
            player.sendMessage("§6§m----------------------------------------------------------------+");
        }
        return true;
    }

    public void sendRequestNotification(Player target, FriendRequest request) {
        if (target == null || request == null) {
            return;
        }

        target.sendMessage("§8➥ " + request.getRequesterName() + " §6wants to add you as a friend.");
        target.spigot().sendMessage(buildRequestComponent(request));
    }

    public void notifyFriendJoin(Player player) {
        if (player == null) {
            return;
        }

        for (UUID friendId : getFriends(player.getUniqueId())) {
            Player friend = Bukkit.getPlayer(friendId);
            if (friend == null || !friend.isOnline() || !canSeeFriend(friendId, player)) {
                continue;
            }

            String color = resolveTagColor(player);
            String displayName = PlaceholderManager.getDisplayName(player);
            friend.sendMessage("§aYour friend " + color + displayName + " §ajoined the server.");
        }
    }

    public String getFriendDisplay(UUID friendId) {
        return getFriendAccountName(friendId);
    }

    public String getFriendAccountName(UUID friendId) {
        if (friendId == null) {
            return "Unknown";
        }

        if (plugin != null && plugin.getAccountController() != null) {
            Account account = plugin.getAccountController().get(friendId);
            if (account != null && account.getName() != null && !account.getName().trim().isEmpty()) {
                return account.getName();
            }
        }

        Player online = Bukkit.getPlayer(friendId);
        if (online != null) {
            return PlaceholderManager.getAccountName(online);
        }

        if (Managers.getDisguiseManager() != null) {
            String originalName = Managers.getDisguiseManager().getOriginalName(friendId);
            if (originalName != null && !originalName.trim().isEmpty()) {
                return originalName;
            }
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(friendId);
        return offline != null && offline.getName() != null ? offline.getName() : "Unknown";
    }

    public String getFriendVisibleName(UUID friendId) {
        if (friendId == null) {
            return "Unknown";
        }

        Player online = Bukkit.getPlayer(friendId);
        if (online != null) {
            return PlaceholderManager.getDisplayName(online);
        }

        return getFriendAccountName(friendId);
    }

    public String getFriendTagColor(UUID friendId) {
        Player online = Bukkit.getPlayer(friendId);
        return online == null ? "§7" : resolveTagColor(online);
    }

    public Collection<UUID> getOnlineFriends(UUID playerId) {
        List<UUID> online = new ArrayList<>();
        for (UUID friendId : getFriends(playerId)) {
            if (isFriendOnlineFor(playerId, friendId)) {
                online.add(friendId);
            }
        }
        return online;
    }

    public boolean isFriendOnlineFor(UUID viewerId, UUID friendId) {
        Player friend = Bukkit.getPlayer(friendId);
        return friend != null && friend.isOnline() && canSeeFriend(viewerId, friend);
    }

    private boolean canSeeFriend(UUID viewerId, Player friend) {
        if (friend == null) {
            return false;
        }

        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer == null) {
            return false;
        }

        return !VanishManager.isVanished(friend) || viewer.hasPermission("kaoscore.staff.vanish");
    }

    private String resolveTagColor(Player player) {
        if (player == null || plugin == null || plugin.getTagManager() == null) {
            return "§7";
        }

        Tag tag = plugin.getTagManager().getTag(player);
        return tag == null || tag.getColor() == null || tag.getColor().trim().isEmpty() ? "§7" : tag.getColor();
    }

    private List<FriendRequest> loadRequests(UUID targetId, boolean forceReload) {
        if (targetId == null) {
            return new ArrayList<>();
        }

        if (forceReload || !requests.containsKey(targetId)) {
            requests.put(targetId, loadRequestsFromDatabase(targetId));
        }

        List<FriendRequest> list = requests.get(targetId);
        if (list == null) {
            list = new ArrayList<>();
            requests.put(targetId, list);
        }

        pruneExpiredRequests(targetId, list);
        return list;
    }

    private List<FriendRequest> loadRequestsFromDatabase(UUID targetId) {
        List<FriendRequest> result = new ArrayList<>();
        if (!hasDatabase()) {
            return result;
        }

        if (isMongo()) {
            try {
                Bson filter = Filters.eq("target_uuid", targetId.toString());
                for (Document doc : getCollection("friend_requests").find(filter)) {
                    result.add(new FriendRequest(
                            UUID.fromString(doc.getString("requester_uuid")),
                            doc.getString("requester_name"),
                            UUID.fromString(doc.getString("target_uuid")),
                            doc.getLong("created_at")
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar pedidos de amizade do MongoDB: " + e.getMessage());
            }
            return result;
        }

        String sql = "SELECT requester_uuid, requester_name, target_uuid, created_at FROM friend_requests WHERE target_uuid = ? ORDER BY created_at DESC";
        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        result.add(new FriendRequest(
                                UUID.fromString(rs.getString("requester_uuid")),
                                rs.getString("requester_name"),
                                UUID.fromString(rs.getString("target_uuid")),
                                rs.getLong("created_at")
                        ));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao carregar pedidos de amizade: " + e.getMessage());
        }

        return result;
    }

    private void pruneExpiredRequests(UUID targetId, List<FriendRequest> list) {
        if (targetId == null || list == null || list.isEmpty()) {
            return;
        }

        List<FriendRequest> expired = new ArrayList<>();
        for (FriendRequest request : new ArrayList<>(list)) {
            if (request.isExpired()) {
                list.remove(request);
                expired.add(request);
            }
        }

        requests.put(targetId, list);
        for (FriendRequest request : expired) {
            deleteRequest(request.getRequesterId(), request.getTargetId());
        }
    }

    private void cleanupExpiredRequests() {
        if (!hasDatabase()) {
            return;
        }

        long threshold = System.currentTimeMillis() - FriendRequest.EXPIRATION_MILLIS;

        if (isMongo()) {
            try {
                getCollection("friend_requests").deleteMany(Filters.lt("created_at", threshold));
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao limpar pedidos de amizade expirados do MongoDB: " + e.getMessage());
            }
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM friend_requests WHERE created_at < ?")) {
            stmt.setLong(1, threshold);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao limpar pedidos de amizade expirados: " + e.getMessage());
        }
    }

    private void addFriendPair(UUID first, UUID second, long since) {
        friends.computeIfAbsent(first, key -> ConcurrentHashMap.newKeySet()).add(second);
        friends.computeIfAbsent(second, key -> ConcurrentHashMap.newKeySet()).add(first);
        saveFriend(first, second, since);
        saveFriend(second, first, since);
    }

    private void saveFriend(UUID playerId, UUID friendId, long since) {
        if (playerId == null || friendId == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Document doc = new Document("player_uuid", playerId.toString())
                        .append("friend_uuid", friendId.toString())
                        .append("friend_name", getFriendAccountName(friendId))
                        .append("since", since);
                Bson filter = Filters.and(
                        Filters.eq("player_uuid", playerId.toString()),
                        Filters.eq("friend_uuid", friendId.toString())
                );
                getCollection("friends").replaceOne(filter, doc, new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar amizade no MongoDB: " + e.getMessage());
            }
            return;
        }

        String sql = "REPLACE INTO friends (player_uuid, friend_uuid, friend_name, since) VALUES (?, ?, ?, ?)";
        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, friendId.toString());
            stmt.setString(3, getFriendAccountName(friendId));
            stmt.setLong(4, since);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao salvar amizade: " + e.getMessage());
        }
    }

    private void deleteFriendPair(UUID playerId, UUID friendId) {
        if (!hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Bson filter = Filters.or(
                        Filters.and(Filters.eq("player_uuid", playerId.toString()), Filters.eq("friend_uuid", friendId.toString())),
                        Filters.and(Filters.eq("player_uuid", friendId.toString()), Filters.eq("friend_uuid", playerId.toString()))
                );
                getCollection("friends").deleteMany(filter);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao remover amizade do MongoDB: " + e.getMessage());
            }
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM friends WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?)")) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, friendId.toString());
            stmt.setString(3, friendId.toString());
            stmt.setString(4, playerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao remover amizade: " + e.getMessage());
        }
    }

    private void saveRequest(FriendRequest request) {
        if (request == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Document doc = new Document("requester_uuid", request.getRequesterId().toString())
                        .append("requester_name", request.getRequesterName())
                        .append("target_uuid", request.getTargetId().toString())
                        .append("created_at", request.getCreatedAt());
                Bson filter = Filters.and(
                        Filters.eq("requester_uuid", request.getRequesterId().toString()),
                        Filters.eq("target_uuid", request.getTargetId().toString())
                );
                getCollection("friend_requests").replaceOne(filter, doc, new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar pedido de amizade no MongoDB: " + e.getMessage());
            }
            return;
        }

        String sql = "INSERT INTO friend_requests (requester_uuid, requester_name, target_uuid, created_at) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE requester_name = VALUES(requester_name), created_at = VALUES(created_at)";
        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getRequesterId().toString());
            stmt.setString(2, request.getRequesterName());
            stmt.setString(3, request.getTargetId().toString());
            stmt.setLong(4, request.getCreatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao salvar pedido de amizade: " + e.getMessage());
        }
    }

    private void removeRequest(UUID targetId, UUID requesterId) {
        if (targetId == null || requesterId == null) {
            return;
        }

        List<FriendRequest> list = loadRequests(targetId, false);
        list.removeIf(request -> request.getRequesterId().equals(requesterId));
        requests.put(targetId, list);
        deleteRequest(requesterId, targetId);
    }

    private void deleteRequest(UUID requesterId, UUID targetId) {
        if (requesterId == null || targetId == null || !hasDatabase()) {
            return;
        }

        if (isMongo()) {
            try {
                Bson filter = Filters.and(
                        Filters.eq("requester_uuid", requesterId.toString()),
                        Filters.eq("target_uuid", targetId.toString())
                );
                getCollection("friend_requests").deleteOne(filter);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao remover pedido de amizade do MongoDB: " + e.getMessage());
            }
            return;
        }

        try (Connection conn = Managers.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM friend_requests WHERE requester_uuid = ? AND target_uuid = ?")) {
            stmt.setString(1, requesterId.toString());
            stmt.setString(2, targetId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao remover pedido de amizade: " + e.getMessage());
        }
    }

    private TextComponent buildRequestComponent(FriendRequest request) {
        TextComponent line = new TextComponent(" §7- §f" + request.getRequesterName() + " ");

        TextComponent accept = new TextComponent("§a§l[ACCEPT]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + request.getRequesterName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to accept").create()));

        TextComponent space = new TextComponent(" ");

        TextComponent deny = new TextComponent("§c§l[DENY]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + request.getRequesterName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClick to deny").create()));

        line.addExtra(accept);
        line.addExtra(space);
        line.addExtra(deny);
        return line;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasDatabase() {
        return Managers.getDatabaseManager() != null;
    }
}