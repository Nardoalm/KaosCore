package com.knowplugins.ysubz.kaoscore.bungee.commands.social;

import com.knowplugins.ysubz.kaoscore.bungee.BaseBungeeCommand;
import com.knowplugins.ysubz.kaoscore.bungee.BungeeCommandUtil;
import com.knowplugins.ysubz.kaoscore.bungee.BungeeSyncMessenger;
import com.knowplugins.ysubz.kaoscore.bungee.TargetResult;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.model.Clan;
import com.knowplugins.ysubz.kaoscore.bungee.model.ClanInvite;
import com.knowplugins.ysubz.kaoscore.bungee.model.ClanRole;
import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class ClanBungeeCommand extends BaseBungeeCommand {

    ClanBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "clan");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (!require(sender, "kaoscore.member.clan", "§cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = Texts.normalize(args[0]);
        if ("criar".equals(sub) || "create".equals(sub)) {
            createClan(player, args);
            return;
        }
        if ("deletar".equals(sub) || "delete".equals(sub)) {
            deleteClan(player);
            return;
        }
        if ("sair".equals(sub) || "leave".equals(sub)) {
            leaveClan(player);
            return;
        }
        if ("convidar".equals(sub) || "invite".equals(sub)) {
            invite(player, args);
            return;
        }
        if ("convites".equals(sub) || "invites".equals(sub)) {
            invites(player);
            return;
        }
        if ("aceitar".equals(sub) || "accept".equals(sub)) {
            accept(player, args);
            return;
        }
        if ("recusar".equals(sub) || "deny".equals(sub)) {
            deny(player, args);
            return;
        }
        if ("expulsar".equals(sub) || "kick".equals(sub)) {
            kick(player, args);
            return;
        }
        if ("promover".equals(sub) || "promote".equals(sub)) {
            promote(player, args);
            return;
        }
        if ("demover".equals(sub) || "demote".equals(sub)) {
            demote(player, args);
            return;
        }
        if ("transferir".equals(sub) || "transfer".equals(sub)) {
            transfer(player, args);
            return;
        }
        if ("cortag".equals(sub)) {
            colorTag(player, args);
            return;
        }
        if ("editar".equals(sub) || "tag".equals(sub)) {
            editTag(player, args);
            return;
        }
        if ("ver".equals(sub) || "info".equals(sub)) {
            info(sender, args);
            return;
        }
        if ("membros".equals(sub) || "members".equals(sub)) {
            members(sender, args);
            return;
        }
        if ("listar".equals(sub) || "list".equals(sub)) {
            list(sender, args);
            return;
        }
        if ("chat".equals(sub)) {
            reply(sender, "§6Use §f/cc <mensagem> §6para falar no chat do clan.");
            return;
        }

        sendHelp(sender);
    }

    private void sendHelp(CommandSender sender) {
        reply(sender, "§eUso do §6/clan§e:");
        reply(sender, " §e• §6/clan criar <nome> <tag>");
        reply(sender, " §e• §6/clan deletar");
        reply(sender, " §e• §6/clan sair");
        reply(sender, " §e• §6/clan convidar <jogador>");
        reply(sender, " §e• §6/clan convites");
        reply(sender, " §e• §6/clan aceitar <nome|tag>");
        reply(sender, " §e• §6/clan recusar <nome|tag>");
        reply(sender, " §e• §6/clan expulsar <jogador>");
        reply(sender, " §e• §6/clan promover <jogador>");
        reply(sender, " §e• §6/clan demover <jogador>");
        reply(sender, " §e• §6/clan transferir <jogador>");
        reply(sender, " §e• §6/clan cortag [cor]");
        reply(sender, " §e• §6/clan editar <nova_tag>");
        reply(sender, " §e• §6/clan ver <nome|tag>");
        reply(sender, " §e• §6/clan membros <nome|tag>");
        reply(sender, " §e• §6/clan listar [pagina]");
    }

    private void createClan(ProxiedPlayer player, String[] args) {
        if (args.length < 3) {
            reply(player, "§cUse: /clan criar <nome> <tag>");
            return;
        }
        if (plugin.getClans().isInClan(player.getUniqueId())) {
            reply(player, "§cVocê já está em um clan.");
            return;
        }

        String name = args[1];
        String tag = args[2].toUpperCase(Locale.ROOT);
        if (tag.length() > 8) {
            reply(player, "§cA tag deve ter no máximo 8 caracteres.");
            return;
        }

        reply(player, plugin.getClans().createClan(name, tag, player.getUniqueId(), player.getName())
                ? "§aClan §f" + name + " §a[" + tag + "] §acriado com sucesso."
                : "§cNome ou tag já utilizado(a), ou valor inválido.");
    }

    private void deleteClan(ProxiedPlayer player) {
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            reply(player, "§cApenas o líder pode deletar o clan.");
            return;
        }
        reply(player, plugin.getClans().deleteClan(clan.getName(), player.getUniqueId())
                ? "§cClan deletado com sucesso."
                : "§cNão foi possível deletar o clan.");
    }

    private void leaveClan(ProxiedPlayer player) {
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (clan.isLeader(player.getUniqueId())) {
            reply(player, "§cVocê é o líder. Use /clan transferir primeiro.");
            return;
        }
        plugin.getClans().removeMember(clan, player.getUniqueId());
        reply(player, "§aVocê saiu do clan.");
    }

    private void invite(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /clan convidar <jogador>");
            return;
        }
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isMod(player.getUniqueId())) {
            reply(player, "§cVocê não tem permissão do clan para convidar.");
            return;
        }
        if (!clan.canInvite()) {
            reply(player, "§cSeu clan já atingiu o limite de membros.");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[1]);
        if (target.getPlayer() == null) {
            reply(player, "§cJogador não está online.");
            return;
        }
        if (target.getUuid().equals(player.getUniqueId())) {
            reply(player, "§cVocê não pode convidar a si mesmo.");
            return;
        }
        if (plugin.getClans().isInClan(target.getUuid())) {
            reply(player, "§cJogador já está em um clan.");
            return;
        }

        plugin.getClans().sendInvite(clan, player.getUniqueId(), target.getUuid());
        String visibleTarget = plugin.getDisguises().getVisibleName(target.getUuid());
        reply(player, "§aConvite enviado para §f" + visibleTarget);
        sendInviteNotification(target.getPlayer(), clan, player.getUniqueId());
    }

    private void invites(ProxiedPlayer player) {
        List<ClanInvite> invites = plugin.getClans().getInvites(player.getUniqueId());
        if (invites.isEmpty()) {
            reply(player, "§cVocê não possui convites pendentes.");
            return;
        }
        reply(player, "§6§m+-------------------§6[ §eConvites §6]§m-------------------+");
        for (ClanInvite invite : invites) {
            player.sendMessage(buildInviteComponent(invite));
        }
        reply(player, "§6§m----------------------------------------------------------+");
    }

    private void accept(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /clan aceitar <nome|tag>");
            return;
        }
        if (plugin.getClans().isInClan(player.getUniqueId())) {
            reply(player, "§cVocê já está em um clan.");
            return;
        }

        String identifier = Texts.join(args, 1);
        if (!plugin.getClans().hasInvite(player.getUniqueId(), identifier)) {
            reply(player, "§cVocê não tem convite para este clan.");
            return;
        }

        Clan clan = plugin.getClans().getClan(identifier);
        if (clan == null) {
            plugin.getClans().removeInvite(player.getUniqueId(), identifier);
            reply(player, "§cEste clan não existe mais. O convite foi removido.");
            return;
        }

        plugin.getClans().addMember(clan, player.getUniqueId(), ClanRole.RECRUIT);
        reply(player, "§aVocê entrou no clan §f" + clan.getName() + " §a" + clan.getFormattedTag());
        for (UUID memberId : clan.getMembers().keySet()) {
            ProxiedPlayer member = ProxyServer.getInstance().getPlayer(memberId);
            if (member != null && !member.getUniqueId().equals(player.getUniqueId())) {
                reply(member, "§a" + plugin.getDisguises().getVisibleName(player.getUniqueId()) + " entrou no clan.");
            }
        }
    }

    private void deny(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /clan recusar <nome|tag>");
            return;
        }
        String identifier = Texts.join(args, 1);
        if (!plugin.getClans().hasInvite(player.getUniqueId(), identifier)) {
            reply(player, "§cVocê não tem convite para este clan.");
            return;
        }
        plugin.getClans().removeInvite(player.getUniqueId(), identifier);
        reply(player, "§cConvite recusado.");
    }

    private void kick(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /clan expulsar <jogador>");
            return;
        }
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isMod(player.getUniqueId())) {
            reply(player, "§cVocê não tem permissão do clan para expulsar.");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[1]);
        if (target.getUuid() == null || !clan.isMember(target.getUuid())) {
            reply(player, "§cJogador não está no clan.");
            return;
        }

        ClanRole actorRole = clan.getRole(player.getUniqueId());
        ClanRole targetRole = clan.getRole(target.getUuid());
        if (actorRole == null || !actorRole.canKick(targetRole)) {
            reply(player, "§cVocê não pode expulsar este jogador.");
            return;
        }

        plugin.getClans().removeMember(clan, target.getUuid());
        String visibleName = plugin.getDisguises().getVisibleName(target.getUuid());
        reply(player, "§c" + visibleName + " foi expulso do clan.");
        if (target.getPlayer() != null) {
            reply(target.getPlayer(), "§cVocê foi expulso do clan §f" + clan.getName());
        }
    }

    private void promote(ProxiedPlayer player, String[] args) {
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            reply(player, "§cApenas o líder pode promover.");
            return;
        }
        if (args.length < 2) {
            reply(player, "§cUse: /clan promover <jogador>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[1]);
        if (target.getUuid() == null || !clan.isMember(target.getUuid())) {
            reply(player, "§cJogador não está no clan.");
            return;
        }

        plugin.getClans().promote(clan, target.getUuid());
        String visibleName = plugin.getDisguises().getVisibleName(target.getUuid());
        reply(player, "§a" + visibleName + " foi promovido.");
        if (target.getPlayer() != null) {
            reply(target.getPlayer(), "§aVocê foi promovido no clan.");
        }
    }

    private void demote(ProxiedPlayer player, String[] args) {
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            reply(player, "§cApenas o líder pode rebaixar.");
            return;
        }
        if (args.length < 2) {
            reply(player, "§cUse: /clan demover <jogador>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[1]);
        if (target.getUuid() == null || !clan.isMember(target.getUuid())) {
            reply(player, "§cJogador não está no clan.");
            return;
        }

        plugin.getClans().demote(clan, target.getUuid());
        String visibleName = plugin.getDisguises().getVisibleName(target.getUuid());
        reply(player, "§c" + visibleName + " foi rebaixado.");
        if (target.getPlayer() != null) {
            reply(target.getPlayer(), "§cVocê foi rebaixado no clan.");
        }
    }

    private void transfer(ProxiedPlayer player, String[] args) {
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            reply(player, "§cApenas o líder pode transferir.");
            return;
        }
        if (args.length < 2) {
            reply(player, "§cUse: /clan transferir <jogador>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[1]);
        if (target.getUuid() == null || !clan.isMember(target.getUuid())) {
            reply(player, "§cJogador não está no clan.");
            return;
        }
        if (target.getUuid().equals(player.getUniqueId())) {
            reply(player, "§cVocê já é o líder deste clan.");
            return;
        }

        plugin.getClans().transfer(clan, target.getUuid());
        String visibleName = plugin.getDisguises().getVisibleName(target.getUuid());
        reply(player, "§aLiderança transferida para §f" + visibleName);
        if (target.getPlayer() != null) {
            reply(target.getPlayer(), "§aVocê agora é o líder do clan.");
        }
    }

    private void colorTag(ProxiedPlayer player, String[] args) {
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            reply(player, "§cApenas o líder pode mudar a cor da tag.");
            return;
        }
        List<String> colors = plugin.getClans().getTagColors();
        if (args.length < 2) {
            TextComponent line = new TextComponent("§aSuas cortag: ");
            for (int index = 0; index < colors.size(); index++) {
                String color = colors.get(index);
                String display = color;
                String token = normalizeColorName(color).replace(' ', '_');
                TextComponent component = new TextComponent(display);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan cortag " + token));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clique para usar " + display).create()));
                line.addExtra(component);
                if (index < colors.size() - 1) {
                    line.addExtra(new TextComponent("§f, "));
                }
            }
            player.sendMessage(line);
            return;
        }

        String normalized = normalizeColorName(Texts.join(args, 1));
        for (String option : colors) {
            if (normalizeColorName(option).equals(normalized)) {
                plugin.getClans().setTagColor(clan, option.substring(0, 2));
                reply(player, "§aCor da tag alterada para: " + option);
                return;
            }
        }
        reply(player, "§cCor não encontrada.");
    }

    private void editTag(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /clan editar <nova_tag>");
            return;
        }
        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(player, "§cVocê não está em um clan.");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            reply(player, "§cApenas o líder pode editar a tag.");
            return;
        }

        String newTag = args[1].toUpperCase(Locale.ROOT);
        if (newTag.length() > 6) {
            reply(player, "§cA tag deve ter no máximo 6 caracteres.");
            return;
        }
        if (plugin.getClans().isTagTaken(newTag, clan.getName())) {
            reply(player, "§cEsta tag já está sendo usada por outro clan.");
            return;
        }

        clan.setTag(newTag);
        plugin.getClans().saveClan(clan);
        reply(player, "§aTag alterada para: " + clan.getFormattedTag());
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            reply(sender, "§cUse: /clan ver <nome|tag>");
            return;
        }
        Clan clan = plugin.getClans().getClan(Texts.join(args, 1));
        if (clan == null) {
            reply(sender, "§cClan não encontrado.");
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        reply(sender, "§6§m+-------------------§6[ §e" + clan.getName() + " §6]§m-------------------+");
        reply(sender, " §7Tag: " + clan.getFormattedTag());
        reply(sender, " §7Líder: §f" + plugin.getPlayerAddresses().findLatestName(clan.getLeader()));
        reply(sender, " §7Membros: §f" + clan.getMemberCount() + "/" + clan.getMaxMembers());
        reply(sender, " §7Criado em: §f" + format.format(new Date(clan.getCreationDate())));
        reply(sender, " §7W/L/ELO: §f" + clan.getWins() + "/" + clan.getLosses() + "/" + clan.getElo());
        reply(sender, "§6§m----------------------------------------------------------+");
    }

    private void members(CommandSender sender, String[] args) {
        if (args.length < 2) {
            reply(sender, "§cUse: /clan membros <nome|tag>");
            return;
        }
        Clan clan = plugin.getClans().getClan(Texts.join(args, 1));
        if (clan == null) {
            reply(sender, "§cClan não encontrado.");
            return;
        }

        reply(sender, "§6§m+-------------------§6[ §eMembros de " + clan.getName() + " §6]§m-------------------+");
        for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
            reply(sender, " §7- " + entry.getValue().getDisplay() + " §f" + plugin.getPlayerAddresses().findLatestName(entry.getKey()));
        }
        reply(sender, "§6§m----------------------------------------------------------+");
    }

    private void list(CommandSender sender, String[] args) {
        Collection<Clan> all = plugin.getClans().getAllClans();
        if (all.isEmpty()) {
            reply(sender, "§cNão há clans criados.");
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        List<Clan> clans = new ArrayList<>(all);
        clans.sort(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (clans.size() + 9) / 10);
        if (page > totalPages) {
            reply(sender, "§cPágina inválida. Máximo: " + totalPages + ".");
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, clans.size());
        reply(sender, "§6§m+-------------------§6[ §eClans §6]§m-------------------+");
        for (int index = start; index < end; index++) {
            Clan clan = clans.get(index);
            reply(sender, " §7" + (index + 1) + ". " + clan.getFormattedTag() + " §f- " + clan.getName());
        }
        reply(sender, "§6§m----------------------------------------------------------+");
        reply(sender, "§7Página §f" + page + " §7de §f" + totalPages);
    }

    private void sendInviteNotification(ProxiedPlayer target, Clan clan, UUID inviterId) {
        if (target == null || clan == null) {
            return;
        }

        String inviterName = plugin.getDisguises().getVisibleName(inviterId);
        Texts.send(target, "§6Você foi convidado para o clan §f" + clan.getName() + " §6" + clan.getFormattedTag());
        Texts.send(target, "§7Convite enviado por: §f" + inviterName);
        target.sendMessage(buildInviteComponent(new ClanInvite(clan.getName(), inviterId, target.getUniqueId())));
    }

    private TextComponent buildInviteComponent(ClanInvite invite) {
        TextComponent line = new TextComponent(" §7- §f" + invite.getClanName() + " ");

        TextComponent accept = new TextComponent("§a[ACEITAR]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan aceitar " + invite.getClanName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para aceitar o convite").create()));

        TextComponent deny = new TextComponent("§c[RECUSAR]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan recusar " + invite.getClanName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClique para recusar o convite").create()));

        line.addExtra(accept);
        line.addExtra(new TextComponent(" "));
        line.addExtra(deny);
        return line;
    }

    private String normalizeColorName(String value) {
        String raw = Texts.stripColors(value == null ? "" : value.replace('_', ' ').replace('-', ' ')).trim().toLowerCase(Locale.ROOT);
        return raw.replaceAll("\\s+", " ");
    }
}
