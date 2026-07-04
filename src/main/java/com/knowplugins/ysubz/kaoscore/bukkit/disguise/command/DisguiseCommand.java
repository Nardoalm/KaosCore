package com.knowplugins.ysubz.kaoscore.bukkit.disguise.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.command.SkinCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.repository.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.repository.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.repository.SkinCacheRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.skin.SkinMatrix;
import com.knowplugins.ysubz.kaoscore.bukkit.util.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.SkinApplier;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager.TagManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DisguiseCommand extends Commands {

    public DisguiseCommand() {
        super("disguise", "nick", "fake");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager == null) {
            player.sendMessage("§cDisguise system unavailable.");
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("list") && player.hasPermission("kaoscore.staff.disguise")) {
            sendDisguiseList(player, disguiseManager);
            return;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("off"))) {
            if (!DisguiseManager.isDisguised(player)) {
                player.sendMessage("§cYou are not disguised.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }
            executeReset(player, disguiseManager);
            player.sendMessage(PlaceholderManager.replacePlaceholders("§aYou are no longer disguised.", player));
            return;
        }

        if (!player.hasPermission("kaoscore.disguise")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String newNick;
        boolean isRandom = false;

        if (args.length == 0) {
            newNick = disguiseManager.getRandomNick();
            isRandom = true;
        } else {
            newNick = args[0];
        }

        if (newNick.length() > 16) {
            player.sendMessage("§cThe nickname cannot be longer than 16 characters.");
            return;
        }

        if (disguiseManager.isNickInUse(newNick)) {
            player.sendMessage("§cThat nickname is already in use.");
            return;
        }

        if (!player.isOp()) {
            if (hasJoinedBefore(newNick)) {
                player.sendMessage("§cThat nickname belongs to a player who has already joined the server.");
                return;
            }
        }

        if (DisguiseManager.isDisguised(player)) {
            executeReset(player, disguiseManager);
        }

        try {
            if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagManager() != null) {
                KaosCore.getInstance().getTagManager().clearDisguiseTag(player.getUniqueId());
            }

            String absoluteRealName = player.getName();
            OfflinePlayer op = Bukkit.getOfflinePlayer(player.getUniqueId());
            if (op != null && op.getName() != null) {
                absoluteRealName = op.getName();
            }
            disguiseManager.setOriginalName(player, absoluteRealName);

            GameProfile profile = ((CraftPlayer) player).getProfile();
            OriginalSkinManager.cache(player, profile);

            changeName(profile, newNick);
            player.setDisplayName(newNick);

            disguiseManager.addInDisguise(player, newNick);

            if (isRandom) {
                SkinMatrix.SkinData data = SkinMatrix.getRandomSkin();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", data.value, data.signature));

                new SkinApplier(player).applySkinForOthers();
                new SkinApplier(player).applySkinForSelf();

                String message = "§aYou are now disguised as §7" + newNick + "§a.";
                player.sendMessage(PlaceholderManager.replacePlaceholders(message, player));

                DisguiseRepository repo = new DisguiseRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                repo.saveAsync(player.getUniqueId(), true, "nick", newNick);

                SkinCacheRepository cacheRepo = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                cacheRepo.saveAsync(newNick, data.value, data.signature);
            } else {
                if (player.hasPermission("kaoscore.staff.disguise")) {
                    String message = "§eSearching for skin for nickname §f" + newNick + "§e...";
                    player.sendMessage(PlaceholderManager.replacePlaceholders(message, player));
                    SkinCommand.setSkin(player, newNick, true);
                } else {
                    new SkinApplier(player).applySkinForOthers();
                    new SkinApplier(player).applySkinForSelf();

                    String message = "§aYou are now disguised as §7" + newNick + "§a.";
                    player.sendMessage(PlaceholderManager.replacePlaceholders(message, player));
                }

                DisguiseRepository repo = new DisguiseRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                repo.saveAsync(player.getUniqueId(), true, "nick", newNick);

                Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(KaosCore.class), () -> {
                    try {
                        GameProfile currentProfile = ((CraftPlayer) player).getProfile();
                        if (currentProfile.getProperties().containsKey("textures")) {
                            Property textures = currentProfile.getProperties().get("textures").iterator().next();
                            if (textures != null) {
                                SkinCacheRepository cacheRepo = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                                cacheRepo.saveAsync(newNick, textures.getValue(), textures.getSignature());
                            }
                        }
                    } catch (Exception ignored) {}
                }, 40L);
            }

            TagManager tagManager = KaosCore.getInstance().getTagManager();
            Tag lowestTag = null;
            int maxPos = -1;

            for (Tag minTag : tagManager.getAvailableTags(player)) {
                if (minTag.getPosition() > maxPos) {
                    maxPos = minTag.getPosition();
                    lowestTag = minTag;
                }
            }

            if (lowestTag != null) {
                tagManager.setTag(player, lowestTag.getId());
            }

            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(KaosCore.class), () -> {
                if (player.isOnline()) {
                    disguiseManager.updateTabName(player);
                }
            }, 5L);

        } catch (Exception exception) {
            player.sendMessage("§cError applying disguise.");
            exception.printStackTrace();
        }
    }

    private void executeReset(Player player, DisguiseManager disguiseManager) {
        try {
            String fakeNick = disguiseManager.getDisguiseNick(player);
            if (fakeNick == null) fakeNick = player.getName();

            String originalName = disguiseManager.popOriginalName(player);
            if (originalName == null || originalName.trim().isEmpty()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(player.getUniqueId());
                originalName = (op != null && op.getName() != null) ? op.getName() : player.getName();
            }

            if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagTeamManager() != null) {
                KaosCore.getInstance().getTagTeamManager().removePlayer(player, fakeNick, originalName);
            }

            GameProfile profile = ((CraftPlayer) player).getProfile();
            changeName(profile, originalName);

            SkinCommand.resetSkin(player, false);
            player.setDisplayName(originalName);

            if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagManager() != null) {
                KaosCore.getInstance().getTagManager().removeTagEntries(player, fakeNick);
                KaosCore.getInstance().getTagManager().clearDisguiseTag(player.getUniqueId());
            }

            disguiseManager.removeDisguise(player);

            new SkinApplier(player).applySkinForSelf();

            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(KaosCore.class), () -> {
                if (player.isOnline()) {
                    disguiseManager.updateTabName(player);
                    if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagTeamManager() != null) {
                        KaosCore.getInstance().getTagTeamManager().refreshPlayer(player);
                    }
                }
            }, 5L);

            DisguiseRepository repo = new DisguiseRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
            repo.saveAsync(player.getUniqueId(), false, null, null);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void changeName(GameProfile profile, String name) throws Exception {
        Field nameField = profile.getClass().getDeclaredField("name");
        nameField.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);
        nameField.set(profile, name);
    }

    private void sendDisguiseList(Player viewer, DisguiseManager disguiseManager) {
        List<Player> disguisedPlayers = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (DisguiseManager.isDisguised(online)) {
                disguisedPlayers.add(online);
            }
        }

        disguisedPlayers.sort(Comparator.comparing(player -> {
            String nick = disguiseManager.getDisguiseNick(player);
            return nick == null || nick.trim().isEmpty() ? player.getName() : nick;
        }, String.CASE_INSENSITIVE_ORDER));

        if (disguisedPlayers.isEmpty()) {
            viewer.sendMessage("§cNo players are disguised at the moment.");
            return;
        }

        viewer.sendMessage("§fDisguised players online: §e" + disguisedPlayers.size());
        for (Player disguised : disguisedPlayers) {
            String fakeNick = disguiseManager.getDisguiseNick(disguised);
            if (fakeNick == null || fakeNick.trim().isEmpty()) {
                fakeNick = disguised.getName();
            }

            String originalName = disguiseManager.getOriginalName(disguised);
            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = Bukkit.getOfflinePlayer(disguised.getUniqueId()).getName();
            }
            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = disguised.getName();
            }

            viewer.sendMessage(" §7- §f" + fakeNick + " §8➔ §e" + originalName);
        }
    }

    private boolean hasJoinedBefore(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return false;
        }

        PlayerAddressRepository repository = MainManager.getPlayerAddressRepository();
        if (repository != null && repository.hasSeenName(nick)) {
            return true;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(nick);
        return offlinePlayer != null && offlinePlayer.hasPlayedBefore();
    }
}