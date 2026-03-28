package com.knowplugins.ysubz.kaoscore.bungee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

final class BungeeFriendService {

    enum RequestResult {
        SENT,
        AUTO_ACCEPTED,
        ALREADY_FRIENDS,
        ALREADY_REQUESTED,
        LIMIT_REACHED,
        SELF,
        INVALID
    }

    private static final int MAX_FRIENDS = 100;

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeePlayerAddressRepository addresses;
    private final ConcurrentHashMap<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();

    BungeeFriendService(BungeeKaosCore plugin, BungeeDatabase database, BungeePlayerAddressRepository addresses) {
        this.plugin = plugin;
        this.database = database;
        this.addresses = addresses;
        reload();
    }

    void reload() {
        friends.clear();
        cleanupExpiredRequests();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT player_uuid, friend_uuid FROM friends");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    UUID playerId = UUID.fromString(resultSet.getString("player_uuid"));
                    UUID friendId = UUID.fromString(resultSet.getString("friend_uuid"));
                    friends.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(friendId);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao carregar amigos: " + exception.getMessage());
        }
    }

    RequestResult createRequest(ProxiedPlayer requester, ProxiedPlayer target) {
        if (requester == null || target == null) {
            return RequestResult.INVALID;
        }
        reload();
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            return RequestResult.SELF;
        }
        if (areFriends(requester.getUniqueId(), target.getUniqueId())) {
            return RequestResult.ALREADY_FRIENDS;
        }
        if (!canAddMoreFriends(requester.getUniqueId()) || !canAddMoreFriends(target.getUniqueId())) {
            return RequestResult.LIMIT_REACHED;
        }
        if (hasPendingRequest(requester.getUniqueId(), target.getUniqueId())) {
            return RequestResult.ALREADY_REQUESTED;
        }
        if (hasPendingRequest(target.getUniqueId(), requester.getUniqueId())) {
            return acceptRequest(requester.getUniqueId(), target.getUniqueId()) ? RequestResult.AUTO_ACCEPTED : RequestResult.LIMIT_REACHED;
        }
        String requesterName = plugin.getDisguises() == null
                ? requester.getName()
                : plugin.getDisguises().getVisibleName(requester.getUniqueId());
        FriendRequest request = new FriendRequest(requester.getUniqueId(), requesterName, target.getUniqueId());
        saveRequest(request);
        return RequestResult.SENT;
    }

    boolean acceptRequest(UUID targetId, UUID requesterId) {
        reload();
        if (targetId == null || requesterId == null || !hasPendingRequest(requesterId, targetId)) {
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
        reload();
        return true;
    }

    void denyRequest(UUID targetId, UUID requesterId) {
        removeRequest(targetId, requesterId);
    }

    void removeFriend(UUID playerId, UUID friendId) {
        if (playerId == null || friendId == null) {
            return;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM friends WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?)")) {
            statement.setString(1, playerId.toString());
            statement.setString(2, friendId.toString());
            statement.setString(3, friendId.toString());
            statement.setString(4, playerId.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao remover amizade: " + exception.getMessage());
        }
        reload();
    }

    boolean areFriends(UUID playerId, UUID friendId) {
        return playerId != null && friendId != null && friends.getOrDefault(playerId, Collections.<UUID>emptySet()).contains(friendId);
    }

    boolean canAddMoreFriends(UUID playerId) {
        return getFriendCount(playerId) < MAX_FRIENDS;
    }

    int getFriendCount(UUID playerId) {
        return getFriends(playerId).size();
    }

    Set<UUID> getFriends(UUID playerId) {
        if (playerId == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(friends.getOrDefault(playerId, Collections.<UUID>emptySet()));
    }

    List<FriendRequest> getRequests(UUID targetId) {
        List<FriendRequest> result = new ArrayList<>();
        if (targetId == null) {
            return result;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT requester_uuid, requester_name, target_uuid, created_at FROM friend_requests WHERE target_uuid = ? ORDER BY created_at DESC")) {
            statement.setString(1, targetId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FriendRequest request = new FriendRequest(
                            UUID.fromString(resultSet.getString("requester_uuid")),
                            resultSet.getString("requester_name"),
                            UUID.fromString(resultSet.getString("target_uuid")),
                            resultSet.getLong("created_at")
                    );
                    if (!request.isExpired()) {
                        result.add(request);
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao carregar pedidos de amizade: " + exception.getMessage());
        }
        return result;
    }

    boolean hasPendingRequest(UUID requesterId, UUID targetId) {
        for (FriendRequest request : getRequests(targetId)) {
            if (request.getRequesterId().equals(requesterId)) {
                return true;
            }
        }
        return false;
    }

    FriendRequest findRequest(UUID targetId, String identifier) {
        String normalized = Texts.normalize(identifier);
        for (FriendRequest request : getRequests(targetId)) {
            if (request.getRequesterId().toString().equalsIgnoreCase(normalized) || Texts.normalize(request.getRequesterName()).equals(normalized) || Texts.normalize(addresses.findLatestName(request.getRequesterId())).equals(normalized)) {
                return request;
            }
        }
        return null;
    }

    FriendRequest getRequest(UUID targetId, UUID requesterId) {
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

    UUID findFriend(UUID ownerId, String identifier) {
        String normalized = Texts.normalize(identifier);
        for (UUID friendId : getFriends(ownerId)) {
            if (friendId.toString().equalsIgnoreCase(normalized)) {
                return friendId;
            }
            String name = addresses.findLatestName(friendId);
            if (Texts.normalize(name).equals(normalized)) {
                return friendId;
            }
        }
        return null;
    }

    Collection<UUID> getOnlineFriends(UUID playerId) {
        List<UUID> result = new ArrayList<>();
        for (UUID friendId : getFriends(playerId)) {
            if (ProxyServer.getInstance().getPlayer(friendId) != null) {
                result.add(friendId);
            }
        }
        return result;
    }

    String getFriendDisplay(UUID friendId) {
        return addresses.findLatestName(friendId);
    }

    private void addFriendPair(UUID first, UUID second, long since) {
        saveFriend(first, second, since);
        saveFriend(second, first, since);
    }

    private void saveFriend(UUID playerId, UUID friendId, long since) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("REPLACE INTO friends (player_uuid, friend_uuid, friend_name, since) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, playerId.toString());
            statement.setString(2, friendId.toString());
            statement.setString(3, addresses.findLatestName(friendId));
            statement.setLong(4, since);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao salvar amizade: " + exception.getMessage());
        }
    }

    private void saveRequest(FriendRequest request) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO friend_requests (requester_uuid, requester_name, target_uuid, created_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE requester_name = VALUES(requester_name), created_at = VALUES(created_at)")) {
            statement.setString(1, request.getRequesterId().toString());
            statement.setString(2, request.getRequesterName());
            statement.setString(3, request.getTargetId().toString());
            statement.setLong(4, request.getCreatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao salvar pedido de amizade: " + exception.getMessage());
        }
    }

    private void removeRequest(UUID targetId, UUID requesterId) {
        if (targetId == null || requesterId == null) {
            return;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM friend_requests WHERE requester_uuid = ? AND target_uuid = ?")) {
            statement.setString(1, requesterId.toString());
            statement.setString(2, targetId.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao remover pedido de amizade: " + exception.getMessage());
        }
    }

    private void cleanupExpiredRequests() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM friend_requests WHERE created_at < ?")) {
            statement.setLong(1, System.currentTimeMillis() - FriendRequest.EXPIRATION_MILLIS);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao limpar pedidos expirados: " + exception.getMessage());
        }
    }
}


