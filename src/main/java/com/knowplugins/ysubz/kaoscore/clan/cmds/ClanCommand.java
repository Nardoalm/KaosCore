package com.knowplugins.ysubz.kaoscore.clan.cmds;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.clan.Clan;
import com.knowplugins.ysubz.kaoscore.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand extends Commands {

    public ClanCommand(KaosCore plugin) {
        super("clan");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "criar":
                cmdCreate(player, args);
                break;
            case "deletar":
                cmdDelete(player);
                break;
            case "sair":
                cmdLeave(player);
                break;
            case "convidar":
                cmdInvite(player, args);
                break;
            case "convites":
                cmdInvites(player);
                break;
            case "aceitar":
                cmdAccept(player, args);
                break;
            case "recusar":
                cmdDeny(player, args);
                break;
            case "expulsar":
                cmdKick(player, args);
                break;
            case "promover":
                cmdPromote(player, args);
                break;
            case "demover":
                cmdDemote(player, args);
                break;
            case "transferir":
                cmdTransfer(player, args);
                break;
            case "cortag":
                cmdColorTag(player, args);
                break;
            case "editar":
                cmdEditTag(player, args);
                break;
            case "ver":
                cmdInfo(player, args);
                break;
            case "membros":
                cmdMembers(player, args);
                break;
            case "listar":
                cmdList(player, args);
                break;
            case "chat":
                cmdChat(player);
                break;
            default:
                sendHelp(player);
                break;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (args.length == 1) {
            List<String> commands = Arrays.asList(
                    "criar",
                    "deletar",
                    "sair",
                    "convidar",
                    "convites",
                    "aceitar",
                    "recusar",
                    "expulsar",
                    "promover",
                    "demover",
                    "transferir",
                    "cortag",
                    "editar",
                    "ver",
                    "membros",
                    "listar",
                    "chat"
            );
            return commands.stream()
                    .filter(command -> command.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("cortag")) {
                return getAvailableTagColors(player).stream()
                        .map(option -> option.token)
                        .filter(token -> token.startsWith(args[args.length - 1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }

            if (Arrays.asList("convidar", "expulsar", "promover", "demover", "transferir").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getDisplayName)
                        .filter(name -> ChatColor.stripColor(name).toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }

            if (Arrays.asList("aceitar", "recusar", "ver", "membros").contains(subCommand)) {
                return Managers.getClanManager().getAllClans().stream()
                        .map(Clan::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eUso do §6/clan§e:");
        sendHelpLine(player, "criar", " §e• §6/clan criar <nome> <tag>");
        sendHelpLine(player, "deletar", " §e• §6/clan deletar");
        sendHelpLine(player, "sair", " §e• §6/clan sair");
        sendHelpLine(player, "convidar", " §e• §6/clan convidar <jogador>");
        sendHelpLine(player, "convites", " §e• §6/clan convites");
        sendHelpLine(player, "aceitar", " §e• §6/clan aceitar <nome|tag>");
        sendHelpLine(player, "recusar", " §e• §6/clan recusar <nome|tag>");
        sendHelpLine(player, "expulsar", " §e• §6/clan expulsar <jogador>");
        sendHelpLine(player, "promover", " §e• §6/clan promover <jogador>");
        sendHelpLine(player, "demover", " §e• §6/clan demover <jogador>");
        sendHelpLine(player, "transferir", " §e• §6/clan transferir <jogador>");
        sendHelpLine(player, "cortag", " §e• §6/clan cortag <cor>");
        sendHelpLine(player, "editar", " §e• §6/clan editar <nova_tag>");
        sendHelpLine(player, "ver", " §e• §6/clan ver <nome|tag>");
        sendHelpLine(player, "membros", " §e• §6/clan membros <nome|tag>");
        sendHelpLine(player, "listar", " §e• §6/clan listar [pagina]");
        sendHelpLine(player, "chat", " §e• §6/clan chat");
        player.sendMessage("");
    }

    private void cmdCreate(Player player, String[] args) {
        if (!requirePermission(player, "criar")) {
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUse: /clan criar <nome> <tag>");
            return;
        }

        String nome = args[1];
        String tag = args[2].toUpperCase(Locale.ROOT);

        if (tag.length() > 8) {
            player.sendMessage("§cA tag deve ter no máximo 8 caracteres!");
            return;
        }

        if (Managers.getClanManager().isInClan(player.getUniqueId())) {
            player.sendMessage("§cVocê já está em um clan!");
            return;
        }

        if (Managers.getClanManager().createClan(nome, tag, player)) {
            player.sendMessage("§aClan §f" + nome + " §a[" + tag + "] §acriado com sucesso!");
            return;
        }

        player.sendMessage("§cNome ou tag já utilizado(a), ou valor inválido!");
    }

    private void cmdDelete(Player player) {
        if (!requirePermission(player, "deletar")) {
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cApenas o líder pode deletar o clan!");
            return;
        }

        if (Managers.getClanManager().deleteClan(clan.getName(), player)) {
            player.sendMessage("§cClan deletado com sucesso!");
        }
    }

    private void cmdLeave(Player player) {
        if (!requirePermission(player, "sair")) {
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cVocê é o líder, não pode sair! Use /clan transferir primeiro.");
            return;
        }

        Managers.getClanManager().removeMember(clan, player.getUniqueId());
        player.sendMessage("§aVocê saiu do clan!");
    }

    private void cmdInvite(Player player, String[] args) {
        if (!requirePermission(player, "convidar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan convidar <jogador>");
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isMod(player.getUniqueId())) {
            player.sendMessage("§cVocê não tem permissão do clan para convidar!");
            return;
        }

        if (!clan.canInvite()) {
            player.sendMessage("§cSeu clan já atingiu o limite de membros!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cVocê não pode convidar a si mesmo!");
            return;
        }

        if (Managers.getClanManager().isInClan(target.getUniqueId())) {
            player.sendMessage("§cJogador já está em um clan!");
            return;
        }

        Managers.getClanManager().sendInvite(clan, player.getUniqueId(), target.getUniqueId());
        String targetColorPrefix = PlaceholderAPI.setPlaceholders(
                target,
                "%kaoscore_tag_color%"
        );
        player.sendMessage("§aConvite enviado para §f" + targetColorPrefix + target.getDisplayName());
        Managers.getClanManager().sendInviteNotification(target, clan, player.getUniqueId());
    }

    private void cmdInvites(Player player) {
        if (!requirePermission(player, "convites")) {
            return;
        }
        Managers.getClanManager().sendInviteList(player, true, false);
    }

    private void cmdAccept(Player player, String[] args) {
        if (!requirePermission(player, "aceitar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan aceitar <nome|tag>");
            return;
        }

        if (Managers.getClanManager().isInClan(player.getUniqueId())) {
            player.sendMessage("§cVocê já está em um clan!");
            return;
        }

        String clanIdentifier = joinArgs(args, 1);
        if (!Managers.getClanManager().hasInvite(player.getUniqueId(), clanIdentifier)) {
            player.sendMessage("§cVocê não tem convite para este clan!");
            return;
        }

        Clan clan = Managers.getClanManager().getClan(clanIdentifier);
        if (clan == null) {
            Managers.getClanManager().removeInvite(player.getUniqueId(), clanIdentifier);
            player.sendMessage("§cEste clan não existe mais. O convite foi removido.");
            return;
        }

        Managers.getClanManager().addMember(clan, player.getUniqueId(), ClanRole.RECRUIT);
        player.sendMessage("§aVocê entrou no clan §f" + clan.getName() + " §a" + clan.getFormattedTag());

        for (UUID memberId : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage("§a" + player.getDisplayName() + " entrou no clan!");
            }
        }
    }

    private void cmdDeny(Player player, String[] args) {
        if (!requirePermission(player, "recusar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan recusar <nome|tag>");
            return;
        }

        String clanIdentifier = joinArgs(args, 1);
        if (!Managers.getClanManager().hasInvite(player.getUniqueId(), clanIdentifier)) {
            player.sendMessage("§cVocê não tem convite para este clan!");
            return;
        }

        Managers.getClanManager().removeInvite(player.getUniqueId(), clanIdentifier);
        player.sendMessage("§cConvite recusado.");
    }

    private void cmdKick(Player player, String[] args) {
        if (!requirePermission(player, "expulsar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan expulsar <jogador>");
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isMod(player.getUniqueId())) {
            player.sendMessage("§cVocê não tem permissão do clan para expulsar!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cJogador não está no clan!");
            return;
        }

        ClanRole targetRole = clan.getRole(target.getUniqueId());
        ClanRole actorRole = clan.getRole(player.getUniqueId());
        if (actorRole == null || !actorRole.canKick(targetRole)) {
            player.sendMessage("§cVocê não pode expulsar este jogador!");
            return;
        }

        Managers.getClanManager().removeMember(clan, target.getUniqueId());
        player.sendMessage("§c" + target.getDisplayName() + " foi expulso do clan!");
        target.sendMessage("§cVocê foi expulso do clan §f" + clan.getName());
    }

    private void cmdPromote(Player player, String[] args) {
        if (!requirePermission(player, "promover")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan promover <jogador>");
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cApenas o líder pode promover!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cJogador não está no clan!");
            return;
        }

        Managers.getClanManager().promote(clan, target.getUniqueId());
        player.sendMessage("§a" + target.getDisplayName() + " foi promovido!");
        target.sendMessage("§aVocê foi promovido no clan!");
    }

    private void cmdDemote(Player player, String[] args) {
        if (!requirePermission(player, "demover")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan demover <jogador>");
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cApenas o líder pode rebaixar!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cJogador não está no clan!");
            return;
        }

        Managers.getClanManager().demote(clan, target.getUniqueId());
        player.sendMessage("§c" + target.getDisplayName() + " foi rebaixado!");
        target.sendMessage("§cVocê foi rebaixado no clan!");
    }

    private void cmdTransfer(Player player, String[] args) {
        if (!requirePermission(player, "transferir")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan transferir <jogador>");
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cApenas o líder pode transferir!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cVocê já é o líder deste clan!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cJogador não está no clan!");
            return;
        }

        Managers.getClanManager().transfer(clan, target.getUniqueId());
        player.sendMessage("§cLiderança transferida para §f" + target.getDisplayName());
        target.sendMessage("§aVocê agora é o líder do clan!");
    }

    private void cmdColorTag(Player player, String[] args) {
        if (!requirePermission(player, "cortag")) {
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cApenas o líder pode mudar a cor da tag!");
            return;
        }

        List<TagColorOption> availableColors = getAvailableTagColors(player);
        if (availableColors.isEmpty()) {
            player.sendMessage("§cVocê não tem permissão para nenhuma cor de tag.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§aSuas cortag:");
            TextComponent line = new TextComponent("");

            for (int i = 0; i < availableColors.size(); i++) {
                TagColorOption option = availableColors.get(i);
                TextComponent colorComponent = new TextComponent(option.colorCode + option.name);
                colorComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan cortag " + option.token));
                colorComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clique para usar a cor §f" + option.name).create()));
                line.addExtra(colorComponent);

                if (i < availableColors.size() - 1) {
                    line.addExtra(new TextComponent("§f, "));
                }
            }

            player.spigot().sendMessage(line);
            return;
        }

        String colorName = normalizeColorName(joinArgs(args, 1));
        TagColorOption selected = null;
        for (TagColorOption option : availableColors) {
            if (option.matches(colorName)) {
                selected = option;
                break;
            }
        }

        if (selected == null) {
            player.sendMessage("§cCor não encontrada ou sem permissão!");
            return;
        }

        Managers.getClanManager().setTagColor(clan, selected.colorCode);
        player.sendMessage("§aCor da tag alterada para: " + selected.colorCode + selected.name);
    }

    private void cmdEditTag(Player player, String[] args) {
        if (!requirePermission(player, "editar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan editar <nova_tag>");
            return;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cApenas o líder pode editar a tag!");
            return;
        }

        String newTag = args[1].toUpperCase(Locale.ROOT);
        if (newTag.length() > 6) {
            player.sendMessage("§cA tag deve ter no máximo 6 caracteres!");
            return;
        }

        if (Managers.getClanManager().isTagTaken(newTag, clan.getName())) {
            player.sendMessage("§cEsta tag já está sendo usada por outro clan!");
            return;
        }

        clan.setTag(newTag);
        Managers.getClanManager().saveClan(clan);
        player.sendMessage("§aTag alterada para: " + clan.getFormattedTag());
    }

    private void cmdInfo(Player player, String[] args) {
        if (!requirePermission(player, "ver")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan ver <nome|tag>");
            return;
        }

        Clan clan = Managers.getClanManager().getClan(joinArgs(args, 1));
        if (clan == null) {
            player.sendMessage("§cClan não encontrado!");
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        player.sendMessage("§6§m+-------------------§6[ §e" + clan.getName() + " §6]§m-------------------+");
        player.sendMessage(" §7Tag: " + clan.getFormattedTag());
        player.sendMessage(" §7Líder: §f" + Bukkit.getOfflinePlayer(clan.getLeader()).getName());
        player.sendMessage(" §7Membros: §f" + clan.getMemberCount() + "/" + clan.getMaxMembers());
        player.sendMessage(" §7Criado em: §f" + format.format(new Date(clan.getCreationDate())));
        player.sendMessage("§6§m----------------------------------------------------------+");
    }

    private void cmdMembers(Player player, String[] args) {
        if (!requirePermission(player, "membros")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan membros <nome|tag>");
            return;
        }

        Clan clan = Managers.getClanManager().getClan(joinArgs(args, 1));
        if (clan == null) {
            player.sendMessage("§cClan não encontrado!");
            return;
        }

        player.sendMessage("§6§m+-------------------§6[ §eMembros de " + clan.getName() + " §6]§m-------------------+");
        for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            player.sendMessage(" §7- " + entry.getValue().getDisplay() + " §f" + playerName);
        }
        player.sendMessage("§6§m----------------------------------------------------------+");
    }

    private void cmdList(Player player, String[] args) {
        if (!requirePermission(player, "listar")) {
            return;
        }

        Collection<Clan> clans = Managers.getClanManager().getAllClans();
        if (clans.isEmpty()) {
            player.sendMessage("§cNão há clans criados.");
            return;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        if (page < 1) {
            page = 1;
        }

        List<Clan> clanList = new ArrayList<>(clans);
        clanList.sort(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (clanList.size() + 9) / 10);
        if (page > totalPages) {
            player.sendMessage("§cPágina inválida. Máximo: " + totalPages + ".");
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, clanList.size());

        player.sendMessage("§6§m+-------------------§6[ §eClans §6]§m-------------------+");
        for (int i = start; i < end; i++) {
            Clan clan = clanList.get(i);
            player.sendMessage(" §7" + (i + 1) + ". " + clan.getFormattedTag() + " §f- " + clan.getName());
        }
        player.sendMessage("§6§m----------------------------------------------------------+");
        player.sendMessage("§7Página §f" + page + " §7de §f" + totalPages);
    }

    private void cmdChat(Player player) {
        if (!requirePermission(player, "chat")) {
            return;
        }

        if (!Managers.getClanManager().isInClan(player.getUniqueId())) {
            player.sendMessage("§cVocê não está em um clan!");
            return;
        }

        player.sendMessage("§6Use §f/cc <mensagem> §6para falar no chat do clan.");
    }

    private void sendHelpLine(Player player, String subCommand, String text) {
        player.sendMessage(text);
    }

    private boolean requirePermission(Player player, String subCommand) {
        return true;
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    private Player resolvePlayer(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return DisguiseManager.getOnlinePlayerByName(name);
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }

    private List<TagColorOption> getAvailableTagColors(Player player) {
        List<TagColorOption> colors = new ArrayList<>();
        for (String raw : Managers.getClanManager().getTagColors()) {
            TagColorOption option = parseTagColor(raw);
            if (option != null && hasPermission(player, option.permission)) {
                colors.add(option);
            }
        }
        colors.sort(Comparator.comparing(option -> option.normalizedName));
        return colors;
    }

    private TagColorOption parseTagColor(String raw) {
        if (raw == null) {
            return null;
        }

        String[] parts = raw.split("\\|", 2);
        String value = parts[0].trim().replace('&', '§');
        if (value.length() < 3 || value.charAt(0) != '§') {
            return null;
        }

        String colorCode = value.substring(0, 2);
        String name = value.substring(2).trim();
        if (name.isEmpty()) {
            return null;
        }

        String normalizedName = normalizeColorName(name);
        String token = normalizedName.replace(' ', '_');
        String permission = parts.length > 1 && !parts[1].trim().isEmpty()
                ? parts[1].trim()
                : "kaoscore.clan.cortag." + token;

        return new TagColorOption(colorCode, name, normalizedName, token, permission);
    }

    private String normalizeColorName(String value) {
        String stripped = ChatColor.stripColor(value == null ? "" : value.replace('&', '§'));
        stripped = stripped.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        return stripped.replaceAll("\\s+", " ");
    }

    private static final class TagColorOption {
        private final String colorCode;
        private final String name;
        private final String normalizedName;
        private final String token;
        private final String permission;

        private TagColorOption(String colorCode, String name, String normalizedName, String token, String permission) {
            this.colorCode = colorCode;
            this.name = name;
            this.normalizedName = normalizedName;
            this.token = token;
            this.permission = permission;
        }

        private boolean matches(String input) {
            return normalizedName.equalsIgnoreCase(input) || token.equalsIgnoreCase(input.replace(' ', '_'));
        }
    }
}

