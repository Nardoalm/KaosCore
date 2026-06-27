package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.vip;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.disguise.SkinMatrix;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

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
            sender.sendMessage("§c§lDISGUISE ➔ §cApenas jogadores podem utilizar este comando.");
            return;
        }

        Player player = (Player) sender;
        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager == null) {
            player.sendMessage("§c§lDISGUISE ➔ §cSistema de disguise indisponível.");
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("list") && player.hasPermission("kaoscore.staff.disguise")) {
            sendDisguiseList(player, disguiseManager);
            return;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("off"))) {
            if (!DisguiseManager.isDisguised(player)) {
                player.sendMessage("§c§lDISGUISE ➔ §cVocê não está disfarçado.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }
            args = new String[0];
        }

        if (args.length == 0 && DisguiseManager.isDisguised(player)) {
            try {
                String previousEntry = player.getName();
                String originalName = disguiseManager.popOriginalName(player);
                if (originalName == null) originalName = player.getName();

                GameProfile profile = ((CraftPlayer) player).getProfile();
                changeName(profile, originalName);

                SkinCommand.resetSkin(player);
                player.setDisplayName(originalName);
                player.setPlayerListName(originalName);

                disguiseManager.removeDisguise(player);
                if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagManager() != null) {
                    KaosCore.getInstance().getTagManager().refreshTag(player, previousEntry);
                }
                new SkinApplier(player).applySkinForSelf();

                String message = "§a§lDISGUISE ➔ §aSeu disfarce foi removido.";
                message = PlaceholderManager.replacePlaceholders(message, player);
                player.sendMessage(message);

                DisguiseRepository repo = new DisguiseRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                repo.saveAsync(player.getUniqueId(), false, null, null);

            } catch (Exception exception) {
                player.sendMessage("§c§lDISGUISE ➔ §cOcorreu um erro ao remover o disfarce.");
                exception.printStackTrace();
            }
            return;
        }

        if (!player.hasPermission("kaoscore.member.disguise")) {
            player.sendMessage("§c§lDISGUISE ➔ §cVocê não possui permissão para executar este comando.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        // LOGICA PARA APLICAR DISFARCE
        String newNick;
        boolean isRandom = false;

        if (args.length == 0) {
            newNick = disguiseManager.getRandomNick();

            isRandom = true;
        } else {
            newNick = args[0];
        }

        if (newNick.length() > 16) {
            player.sendMessage("§c§lDISGUISE ➔ §cO nick não pode ter mais de 16 caracteres.");
            return;
        }

        if (disguiseManager.isNickInUse(newNick)) {
            player.sendMessage("§c§lDISGUISE ➔ §cEsse nick já está em uso.");
            return;
        }

        if (hasJoinedBefore(newNick)) {
            player.sendMessage("§c§lDISGUISE ➔ §cEsse nick pertence a um jogador que já entrou no servidor.");
            return;
        }

        try {
            String previousEntry = player.getName();
            disguiseManager.recordOriginalName(player);
            GameProfile profile = ((CraftPlayer) player).getProfile();
            OriginalSkinManager.cache(player, profile);

            changeName(profile, newNick);

            player.setDisplayName(newNick);
            player.setPlayerListName(newNick);
            if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagManager() != null) {
                KaosCore.getInstance().getTagManager().refreshTag(player, previousEntry);
            }

            if (isRandom) {
                SkinMatrix.SkinData data = SkinMatrix.getRandomSkin();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", data.value, data.signature));

                if (Managers.getDatabaseManager() != null) {
                    SkinRepository repo = new SkinRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                    repo.saveAsync(player.getUniqueId(), "random-disguise", data.value, data.signature);
                }

                new SkinApplier(player).applySkinForOthers();

                String message = "§a§lDISGUISE ➔ §aDisfarce aleatório aplicado: §e" + newNick;
                message = PlaceholderManager.replacePlaceholders(message, player);
                player.sendMessage(message);
            } else {
                if (player.hasPermission("kaoscore.staff.disguise")) {
                    String message = "§eBuscando skin para o nick §f" + newNick + "§e...";
                    message = PlaceholderManager.replacePlaceholders(message, player);
                    player.sendMessage(message);
                    SkinCommand.setSkinForOthers(player, newNick);
                } else {
                    new SkinApplier(player).applySkinForOthers();

                    String message = "§a§lDISGUISE ➔ §aVocê agora é: §e" + newNick;
                    message = PlaceholderManager.replacePlaceholders(message, player);
                    player.sendMessage(message);
                }
            }

            disguiseManager.addInDisguise(player, newNick);
            disguiseManager.updateTabName(player);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

            DisguiseRepository repo = new DisguiseRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
            repo.saveAsync(player.getUniqueId(), true, "nick", newNick);

        } catch (Exception exception) {
            player.sendMessage("§c§lDISGUISE ➔ §cErro ao aplicar disfarce.");
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
            viewer.sendMessage("§e§lFAKE ➔ §cNenhum jogador está disfarçado no momento.");
            return;
        }

        viewer.sendMessage("§e§lFAKE ➔ §fJogadores disfarçados online: §e" + disguisedPlayers.size());
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

            viewer.sendMessage(" §7- §f" + fakeNick + " §8-> §e" + originalName);
        }
    }

    private boolean hasJoinedBefore(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return false;
        }

        PlayerAddressRepository repository = Managers.getPlayerAddressRepository();
        if (repository != null && repository.hasSeenName(nick)) {
            return true;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(nick);
        return offlinePlayer != null && offlinePlayer.hasPlayedBefore();
    }
}

