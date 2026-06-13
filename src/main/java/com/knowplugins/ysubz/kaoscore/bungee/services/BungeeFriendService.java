package com.knowplugins.ysubz.kaoscore.bungee.services;

import com.knowplugins.ysubz.kaoscore.bungee.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.FriendRequest;
import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.repository.BungeePlayerAddressRepository;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeFriendService {

    public enum RequestResult {
        SENT,
        AUTO_ACCEPTED,
        ALREADY_FRIENDS,
        ALREADY_REQUESTED,
        LIMIT_REACHED,
        SELF,
        FAILED
    }

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeePlayerAddressRepository playerAddresses;
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, FriendRequest>> requests = new ConcurrentHashMap<>();
    private static final int FRIEND_LIMIT = 100;

    public BungeeFriendService(BungeeKaosCore plugin, BungeeDatabase database, BungeePlayerAddressRepository playerAddresses) {
        this.plugin = plugin;
        this.database = database;
        this.playerAddresses = playerAddresses;
    }

    public RequestResult createRequest(ProxiedPlayer requester, ProxiedPlayer target) {
        if (requester == null || target == null) {
            return RequestResult.FAILED;
        }
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            return RequestResult.SELF;
        }
        if (areFriends(requester.getUniqueId(), target.getUniqueId())) {
            return RequestResult.ALREADY_FRIENDS;
        }
        if (getFriends(requester.getUniqueId()).size() >= FRIEND_LIMIT || getFriends(target.getUniqueId()).size() >= FRIEND_LIMIT) {
            return RequestResult.LIMIT_REACHED;
        }

        FriendRequest reverse = getRequest(requester.getUniqueId(), target.getUniqueId());
        if (reverse != null) {
            acceptRequest(requester.getUniqueId(), target.getUniqueId());
            return RequestResult.AUTO_ACCEPTED;
        }

        Map<UUID, FriendRequest> targetRequests = requests.computeIfAbsent(target.getUniqueId(), key -> new ConcurrentHashMap<>());
        if (targetRequests.containsKey(requester.getUniqueId())) {
            return RequestResult.ALREADY_REQUESTED;
        }
        targetRequests.put(requester.getUniqueId(), new FriendRequest(requester.getUniqueId(), requester.getName(), target.getUniqueId()));
        return RequestResult.SENT;
    }

    public FriendRequest getRequest(UUID targetId, UUID requesterId) {
        Map<UUID, FriendRequest> targetRequests = requests.get(targetId);
        if (targetRequests == null) {
            return null;
        }
        FriendRequest request = targetRequests.get(requesterId);
        return request == null || request.isExpired() ? null : request;
    }

    public List<FriendRequest> getRequests(UUID targetId) {
        Map<UUID, FriendRequest> targetRequests = requests.get(targetId);
        if (targetRequests == null) {
            return Collections.emptyList();
        }
        List<FriendRequest> result = new ArrayList<>();
        for (FriendRequest request : targetRequests.values()) {
            if (request != null && !request.isExpired()) {
                result.add(request);
            }
        }
        result.sort(Comparator.comparingLong(FriendRequest::getCreatedAt).reversed());
        return result;
    }

    public FriendRequest findRequest(UUID targetId, String requester) {
        String normalized = Texts.normalize(requester);
        if (normalized.isEmpty()) {
            return null;
        }
        for (FriendRequest request : getRequests(targetId)) {
            if (normalized.equals(Texts.normalize(request.getRequesterName())) || normalized.equals(request.getRequesterId().toString())) {
                return request;
            }
            if (normalized.equals(Texts.normalize(getFriendDisplay(request.getRequesterId())))) {
                return request;
            }
        }
        return null;
    }

    public boolean acceptRequest(UUID targetId, UUID requesterId) {
        FriendRequest request = getRequest(targetId, requesterId);
        if (request == null) {
            return false;
        }
        removeRequest(targetId, requesterId);
        addFriendship(targetId, requesterId);
        return true;
    }

    public void denyRequest(UUID targetId, UUID requesterId) {
        removeRequest(targetId, requesterId);
    }

    public UUID findFriend(UUID ownerId, String friendQuery) {
        String normalized = Texts.normalize(friendQuery);
        if (normalized.isEmpty()) {
            return null;
        }
        for (UUID friendId : getFriends(ownerId)) {
            if (normalized.equals(Texts.normalize(getFriendDisplay(friendId))) || normalized.equals(friendId.toString())) {
                return friendId;
            }
        }
        return null;
    }

    public void removeFriend(UUID ownerId, UUID friendId) {
        if (ownerId == null || friendId == null) {
            return;
        }
        Set<UUID> ownerFriends = friends.get(ownerId);
        if (ownerFriends != null) {
            ownerFriends.remove(friendId);
        }
        Set<UUID> friendFriends = friends.get(friendId);
        if (friendFriends != null) {
            friendFriends.remove(ownerId);
        }
    }

    public Set<UUID> getFriends(UUID playerId) {
        Set<UUID> playerFriends = friends.get(playerId);
        if (playerFriends == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(playerFriends);
    }

    public Collection<UUID> getOnlineFriends(UUID playerId) {
        List<UUID> result = new ArrayList<>();
        for (UUID friendId : getFriends(playerId)) {
            if (ProxyServer.getInstance().getPlayer(friendId) != null) {
                result.add(friendId);
            }
        }
        return result;
    }

    public String getFriendDisplay(UUID friendId) {
        if (friendId == null) {
            return "Desconhecido";
        }
        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(friendId);
        if (online != null) {
            return online.getName();
        }
        return playerAddresses.findLatestName(friendId);
    }

    public FriendRequest getRequest(UUID targetId, String requesterName) {
        String normalized = Texts.normalize(requesterName);
        for (FriendRequest request : getRequests(targetId)) {
            if (normalized.equals(Texts.normalize(request.getRequesterName()))) {
                return request;
            }
        }
        return null;
    }

    private boolean areFriends(UUID left, UUID right) {
        Set<UUID> leftFriends = friends.get(left);
        return leftFriends != null && leftFriends.contains(right);
    }

    private void addFriendship(UUID left, UUID right) {
        friends.computeIfAbsent(left, key -> ConcurrentHashMap.<UUID>newKeySet()).add(right);
        friends.computeIfAbsent(right, key -> ConcurrentHashMap.<UUID>newKeySet()).add(left);
    }

    private void removeRequest(UUID targetId, UUID requesterId) {
        Map<UUID, FriendRequest> targetRequests = requests.get(targetId);
        if (targetRequests != null) {
            targetRequests.remove(requesterId);
            if (targetRequests.isEmpty()) {
                requests.remove(targetId);
            }
        }
    }
}
