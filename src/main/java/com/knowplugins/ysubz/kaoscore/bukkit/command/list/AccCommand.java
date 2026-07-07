package com.knowplugins.ysubz.kaoscore.bukkit.command.list;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.PermissionGroup;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AccCommand extends Commands {

    public AccCommand() {
        super("acc", "account", "playerinfo");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        UUID targetUuid;
        String targetName;
        boolean isSelf = false;

        if (args.length < 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /acc <player>");
                return;
            }
            Player player = (Player) sender;
            targetUuid = player.getUniqueId();
            targetName = player.getName();
            isSelf = true;
        } else {
            if (!sender.hasPermission("kaoscore.command.acc.others") && !sender.isOp()) {
                sender.sendMessage("§cYou do not have permission to view other players' account information.");
                ModerationCommandUtil.play(sender, Sound.CLICK);
                return;
            }
            ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
            targetUuid = target.getUuid();
            targetName = target.getName();
            if (targetUuid == null) {
                sender.sendMessage("§cPlayer not found.");
                ModerationCommandUtil.play(sender, Sound.CLICK);
                return;
            }
        }

        DatabaseManager db = MainManager.getDatabaseManager();
        if (db == null) {
            sender.sendMessage("§cDatabase offline.");
            return;
        }

        final boolean isOnline = Bukkit.getPlayer(targetUuid) != null && Bukkit.getPlayer(targetUuid).isOnline();

        // Run asynchronously to avoid lag spikes from database queries
        Bukkit.getScheduler().runTaskAsynchronously(KaosCore.getInstance(), () -> {
            long addressId = 0;
            long registeredAt = 0;
            long lastLogin = 0;
            boolean premium = false;

            // 1. Fetch address ID (first address entry ID)
            if (!db.isMongo()) {
                try (Connection conn = db.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id FROM player_addresses WHERE player_uuid = ? ORDER BY id ASC LIMIT 1")) {
                    ps.setString(1, targetUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            addressId = rs.getLong("id");
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                addressId = Math.abs(targetUuid.hashCode() % 10000);
            }

            // 2. Fetch auth info (premium, registered_at, last_login)
            if (db.isMongo()) {
                try {
                    MongoCollection<Document> coll = db.getMongoDatabase().getCollection("kaoscore_auth");
                    Document doc = coll.find(Filters.eq("name", targetName)).first();
                    if (doc != null) {
                        lastLogin = doc.containsKey("last_login") ? doc.getLong("last_login") : 0L;
                        premium = doc.getBoolean("is_premium", false);

                        MongoCollection<Document> addrColl = db.getMongoDatabase().getCollection("player_addresses");
                        Document firstAddr = addrColl.find(Filters.eq("player_uuid", targetUuid.toString())).sort(Sorts.ascending("first_seen")).first();
                        if (firstAddr != null) {
                            registeredAt = firstAddr.getLong("first_seen");
                        } else {
                            registeredAt = lastLogin;
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                try (Connection conn = db.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT is_premium, registered_at, last_login FROM kaoscore_auth WHERE name = ?")) {
                    ps.setString(1, targetName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            premium = rs.getBoolean("is_premium");
                            registeredAt = rs.getLong("registered_at");
                            lastLogin = rs.getLong("last_login");
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Fallback for dates if not found in auth table
            if (registeredAt == 0 || lastLogin == 0) {
                if (db.isMongo()) {
                    try {
                        MongoCollection<Document> addrColl = db.getMongoDatabase().getCollection("player_addresses");
                        Document firstAddr = addrColl.find(Filters.eq("player_uuid", targetUuid.toString())).sort(Sorts.ascending("first_seen")).first();
                        if (firstAddr != null) {
                            registeredAt = firstAddr.getLong("first_seen");
                        }
                        Document lastAddr = addrColl.find(Filters.eq("player_uuid", targetUuid.toString())).sort(Sorts.descending("last_seen")).first();
                        if (lastAddr != null) {
                            lastLogin = lastAddr.getLong("last_seen");
                        }
                    } catch (Exception ignored) {}
                } else {
                    try (Connection conn = db.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT MIN(first_seen) as reg, MAX(last_seen) as lst FROM player_addresses WHERE player_uuid = ?")) {
                        ps.setString(1, targetUuid.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                registeredAt = rs.getLong("reg");
                                lastLogin = rs.getLong("lst");
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 3. Fetch current active grants
            List<GrantInfo> grants = new ArrayList<>();
            if (db.isMongo()) {
                try {
                    MongoCollection<Document> coll = db.getMongoDatabase().getCollection("kaoscore_user_grants");
                    Bson filter = Filters.and(
                        Filters.or(
                            Filters.eq("uuid", targetUuid.toString()),
                            Filters.regex("player_name", "^" + targetName + "$", "i")
                        ),
                        Filters.eq("active", true)
                    );
                    for (Document doc : coll.find(filter)) {
                        String groupName = doc.getString("group_name");
                        String givenBy = doc.getString("given_by_name");
                        long createdAt = doc.getLong("created_at");
                        long expiresAt = doc.containsKey("expires_at") ? doc.getLong("expires_at") : 0L;
                        grants.add(new GrantInfo(groupName, givenBy, createdAt, expiresAt));
                    }
                } catch (Exception ignored) {}
            } else {
                try (Connection conn = db.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT group_name, given_by_name, created_at, expires_at FROM kaoscore_user_grants WHERE (uuid = ? OR LOWER(player_name) = ?) AND active = 1")) {
                    ps.setString(1, targetUuid.toString());
                    ps.setString(2, targetName.toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String groupName = rs.getString("group_name");
                            String givenBy = rs.getString("given_by_name");
                            long createdAt = rs.getLong("created_at");
                            long expiresAt = rs.getLong("expires_at");
                            grants.add(new GrantInfo(groupName, givenBy, createdAt, expiresAt));
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 4. Merge with online/offline permission data to guarantee accuracy
            if (isOnline) {
                com.knowplugins.ysubz.kaoscore.bukkit.permission.EffectivePermissions effective = MainManager.getPermissionManager().getEffective(targetUuid);
                if (effective != null) {
                    for (String group : effective.getDirectGroups()) {
                        boolean found = false;
                        for (GrantInfo info : grants) {
                            if (info.groupName.equalsIgnoreCase(group)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            grants.add(new GrantInfo(group, "SYSTEM", 0, 0));
                        }
                    }
                }
            } else {
                try {
                    com.knowplugins.ysubz.kaoscore.bukkit.permission.data.PermissionUserData data = MainManager.getPermissionManager().getRepository().loadUser(targetUuid, targetName);
                    if (data != null && data.getGrants() != null) {
                        for (com.knowplugins.ysubz.kaoscore.bukkit.permission.GroupGrant grant : data.getGrants()) {
                            boolean found = false;
                            for (GrantInfo info : grants) {
                                if (info.groupName.equalsIgnoreCase(grant.getGroupName())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                grants.add(new GrantInfo(grant.getGroupName(), "SYSTEM", grant.getCreatedAt(), grant.getExpiresAt()));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Compile information and format it
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Sao_Paulo"));

            String regStr = registeredAt > 0 ? sdf.format(new Date(registeredAt)) : "Não registrado";
            String lastStr = lastLogin > 0 ? sdf.format(new Date(lastLogin)) : "Nenhum registro";

            // Setup premium status detection
            boolean premiumDetected = premium || targetUuid.version() == 4;
            if (isOnline) {
                AuthProfile cached = KaosCore.getInstance().getAuthManager().getCachedProfile(targetUuid);
                if (cached != null && cached.isPremium()) {
                    premiumDetected = true;
                }
            }

            String finalTargetName = targetName;
            boolean finalPremium = premiumDetected;
            long finalAddressId = addressId;

            Bukkit.getScheduler().runTask(KaosCore.getInstance(), () -> {
                sender.sendMessage("§8§m---------------------------------------------");
                sender.sendMessage("§b§lCONTA ➔ §f" + finalTargetName);
                sender.sendMessage("§8§m---------------------------------------------");
                sender.sendMessage(" §7• §eID de Registro: §f#" + finalAddressId);
                sender.sendMessage(" §7• §eConexão: " + (isOnline ? "§aOnline" : "§cOffline"));
                sender.sendMessage(" §7• §eUUID: §7" + targetUuid);
                sender.sendMessage(" §7• §eConta Original: " + (finalPremium ? "§aSim" : "§cNão"));
                sender.sendMessage(" §7• §eVínculo Discord: §cNão vinculado");
                sender.sendMessage(" §7• §eData de Cadastro: §f" + regStr);
                sender.sendMessage(" §7• §eÚltimo Acesso: §f" + lastStr);
                sender.sendMessage("§8§m---------------------------------------------");
                sender.sendMessage("§b§lCARGOS E PERMISSÕES:");

                if (grants.isEmpty()) {
                    sender.sendMessage(" §7• §7Membro §8(Padrão)");
                } else {
                    for (GrantInfo info : grants) {
                        PermissionGroup groupObj = MainManager.getPermissionManager().getGroup(info.groupName);
                        String displayName = info.groupName.toUpperCase();
                        if (groupObj != null && groupObj.getPrefix() != null && !groupObj.getPrefix().isEmpty()) {
                            displayName = ChatColor.translateAlternateColorCodes('&', groupObj.getPrefix().trim());
                        }
                        sender.sendMessage(" §7• " + displayName + " §8(Ativo)");
                        String dateStr = info.createdAt > 0 ? sdf.format(new Date(info.createdAt)) : "N/A";
                        sender.sendMessage("   §8└ §7Ativado em: §f" + dateStr + " §7por: §f" + info.givenBy);
                    }
                }
                sender.sendMessage("§8§m---------------------------------------------");

                ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
            });
        });
    }

    private static class GrantInfo {
        String groupName;
        String givenBy;
        long createdAt;
        long expiresAt;

        GrantInfo(String groupName, String givenBy, long createdAt, long expiresAt) {
            this.groupName = groupName;
            this.givenBy = givenBy;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }
}
