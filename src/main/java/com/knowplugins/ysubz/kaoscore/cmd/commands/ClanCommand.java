package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.clan.Clan;
import com.knowplugins.ysubz.kaoscore.clan.ClanInvite;
import com.knowplugins.ysubz.kaoscore.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

public class ClanCommand extends Commands implements TabCompleter {

    private final KaosCore plugin;

    public ClanCommand(KaosCore plugin) {
        super("clan");
        this.plugin = plugin;
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

        switch (args[0].toLowerCase()) {
            case "criar":
                cmdCreate(player, args);
                break;
            case "deletar":
                cmdDelete(player, args);
                break;
            case "sair":
                cmdLeave(player, args);
                break;
            case "convidar":
                cmdInvite(player, args);
                break;
            case "convites":
                cmdInvites(player, args);
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
                cmdChat(player, args);
                break;
            default:
                sendHelp(player);
                break;
        }
    }

    private void sendHelp(Player p) {
        p.sendMessage("§5§m+-------------------§5[ §dComandos Clan §5]§m-------------------+");
        p.sendMessage("§d/clan criar <nome> <tag> §7- Criar um clan");
        p.sendMessage("§d/clan deletar §7- Deletar seu clan");
        p.sendMessage("§d/clan sair §7- Sair do clan");
        p.sendMessage("§d/clan convidar <jogador> §7- Convidar jogador");
        p.sendMessage("§d/clan convites §7- Ver seus convites");
        p.sendMessage("§d/clan aceitar <tag> §7- Aceitar convite");
        p.sendMessage("§d/clan recusar <tag> §7- Recusar convite");
        p.sendMessage("§d/clan expulsar <jogador> §7- Expulsar membro");
        p.sendMessage("§d/clan promover <jogador> §7- Promover membro");
        p.sendMessage("§d/clan demover <jogador> §7- Rebaixar membro");
        p.sendMessage("§d/clan transferir <jogador> §7- Transferir liderança");
        p.sendMessage("§d/clan cortag <cor> §7- Mudar cor da tag");
        p.sendMessage("§d/clan editar <nova_tag> §7- Editar tag");
        p.sendMessage("§d/clan ver <tag> §7- Ver informações");
        p.sendMessage("§d/clan membros <tag> §7- Ver membros");
        p.sendMessage("§d/clan listar §7- Listar clans");
        p.sendMessage("§d/clan chat §7- Entrar no chat do clan");
        p.sendMessage("§5§m----------------------------------------------------------+");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;

        if (args.length == 1) {
            return Arrays.asList("criar", "deletar", "sair", "convidar", "convites",
                            "aceitar", "recusar", "expulsar", "promover", "demover",
                            "transferir", "cortag", "editar", "ver", "membros",
                            "listar", "chat").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("cortag")) {
                return Managers.getClanManager().getTagColors().stream()
                         .map(this::extractColorName)
                         .filter(name -> !name.isEmpty())
                         .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                         .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("convidar") || args[0].equalsIgnoreCase("expulsar") ||
                    args[0].equalsIgnoreCase("promover") || args[0].equalsIgnoreCase("demover") ||
                    args[0].equalsIgnoreCase("transferir")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("aceitar") || args[0].equalsIgnoreCase("recusar") ||
                    args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("membros")) {
                return Managers.getClanManager().getAllClans().stream()
                         .map(Clan::getName)
                         .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                         .collect(Collectors.toList());
            }
        }

        return null;
    }

    private boolean cmdCreate(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage("§cUse: /clan criar <nome> <tag>");
            return true;
        }

        String nome = args[1];
        String tag = args[2].toUpperCase();

        if (tag.length() > 6) {
            p.sendMessage("§cA tag deve ter no máximo 6 caracteres!");
            return true;
        }

        if (Managers.getClanManager().isInClan(p.getUniqueId())) {
            p.sendMessage("§cVocê já está em um clan!");
            return true;
        }

        if (Managers.getClanManager().createClan(nome, tag, p)) {
            p.sendMessage("§aClan §f" + nome + " §a[" + tag + "] §acriado com sucesso!");
        } else {
            p.sendMessage("§cNome já utilizado ou inválido!");
        }
        return true;
    }

    private boolean cmdDelete(Player p, String[] args) {
        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cApenas o líder pode deletar o clan!");
            return true;
        }

        if (Managers.getClanManager().deleteClan(clan.getName(), p)) {
            p.sendMessage("§cClan deletado com sucesso!");
        }
        return true;
    }

    private boolean cmdLeave(Player p, String[] args) {
        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cO líder não pode sair! Use /clan transferir primeiro.");
            return true;
        }

        Managers.getClanManager().removeMember(clan, p.getUniqueId());
        p.sendMessage("§aVocê saiu do clan!");
        return true;
    }

    private boolean cmdInvite(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan convidar <jogador>");
            return true;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isMod(p.getUniqueId())) {
            p.sendMessage("§cVocê não tem permissão para convidar!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage("§cJogador não encontrado!");
            return true;
        }

        if (Managers.getClanManager().isInClan(target.getUniqueId())) {
            p.sendMessage("§cJogador já está em um clan!");
            return true;
        }

        Managers.getClanManager().sendInvite(clan, p.getUniqueId(), target.getUniqueId());

        p.sendMessage("§aConvite enviado para §f" + target.getName());
        target.sendMessage("§aVocê foi convidado para o clan §f" + clan.getName() + " §a[" + clan.getTag() + "]");
        target.sendMessage("§aUse §f/clan aceitar " + clan.getName() + " §aou §f/clan recusar " + clan.getName());
        return true;
    }

    private boolean cmdInvites(Player p, String[] args) {
        List<ClanInvite> invites = Managers.getClanManager().getInvites(p.getUniqueId());

        if (invites.isEmpty()) {
            p.sendMessage("§cVocê não tem convites pendentes.");
            return true;
        }

        p.sendMessage("§6§m+-------------------§6[ §eConvites §6]§m-------------------+");
        for (ClanInvite invite : invites) {
            if (!invite.isExpired()) {
                p.sendMessage(" §7- §f" + invite.getClanName());
            }
        }
        p.sendMessage("§6§m------------------------------------------------+");
        return true;
    }

    private boolean cmdAccept(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan aceitar <tag>");
            return true;
        }

        String clanName = args[1];
        Clan clan = Managers.getClanManager().getClan(clanName);
        if (clan == null) {
            p.sendMessage("§cClan não encontrado!");
            return true;
        }

        List<ClanInvite> invites = Managers.getClanManager().getInvites(p.getUniqueId());
        boolean hasInvite = invites.stream().anyMatch(i -> i.getClanName().equalsIgnoreCase(clanName));

        if (!hasInvite) {
            p.sendMessage("§cVocê não tem convite para este clan!");
            return true;
        }

        Managers.getClanManager().addMember(clan, p.getUniqueId(), ClanRole.RECRUIT);
        Managers.getClanManager().removeInvite(p.getUniqueId(), clanName);

        p.sendMessage("§aVocê entrou no clan §f" + clan.getName() + " §a[" + clan.getTag() + "]");

        for (UUID memberId : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage("§a" + p.getName() + " entrou no clan!");
            }
        }
        return true;
    }

    private boolean cmdDeny(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan recusar <tag>");
            return true;
        }

        String clanName = args[1];
        Managers.getClanManager().removeInvite(p.getUniqueId(), clanName);
        p.sendMessage("§cConvite recusado.");
        return true;
    }

    private boolean cmdKick(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan expulsar <jogador>");
            return true;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isMod(p.getUniqueId())) {
            p.sendMessage("§cVocê não tem permissão para expulsar!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage("§cJogador não encontrado!");
            return true;
        }

        if (!clan.isMember(target.getUniqueId())) {
            p.sendMessage("§cJogador não está no clan!");
            return true;
        }

        ClanRole targetRole = clan.getRole(target.getUniqueId());
        if (!clan.getRole(p.getUniqueId()).canKick(targetRole)) {
            p.sendMessage("§cVocê não pode expulsar este jogador!");
            return true;
        }

        Managers.getClanManager().removeMember(clan, target.getUniqueId());
        p.sendMessage("§c" + target.getName() + " foi expulso do clan!");
        target.sendMessage("§cVocê foi expulso do clan §f" + clan.getName());
        return true;
    }

    private boolean cmdPromote(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan promover <jogador>");
            return true;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cApenas o líder pode promover!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage("§cJogador não encontrado!");
            return true;
        }

        if (!clan.isMember(target.getUniqueId())) {
            p.sendMessage("§cJogador não está no clan!");
            return true;
        }

        Managers.getClanManager().promote(clan, target.getUniqueId());
        p.sendMessage("§a" + target.getName() + " foi promovido!");
        target.sendMessage("§aVocê foi promovido no clan!");
        return true;
    }

    private boolean cmdDemote(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan demover <jogador>");
            return true;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cApenas o líder pode rebaixar!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage("§cJogador não encontrado!");
            return true;
        }

        if (!clan.isMember(target.getUniqueId())) {
            p.sendMessage("§cJogador não está no clan!");
            return true;
        }

        Managers.getClanManager().demote(clan, target.getUniqueId());
        p.sendMessage("§c" + target.getName() + " foi rebaixado!");
        target.sendMessage("§cVocê foi rebaixado no clan!");
        return true;
    }

    private boolean cmdTransfer(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan transferir <jogador>");
            return true;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cApenas o líder pode transferir!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage("§cJogador não encontrado!");
            return true;
        }

        if (!clan.isMember(target.getUniqueId())) {
            p.sendMessage("§cJogador não está no clan!");
            return true;
        }

        Managers.getClanManager().transfer(clan, target.getUniqueId());
        p.sendMessage("§cLiderança transferida para §f" + target.getName());
        target.sendMessage("§aVocê agora é o líder do clan!");
        return true;
    }

    private boolean cmdColorTag(Player p, String[] args) {
        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cApenas o líder pode mudar a cor da tag!");
            return true;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSuas cortag:"));

            List<TagColorOption> colors = getSortedTagColors();
            TextComponent line = new TextComponent("");

            for (int i = 0; i < colors.size(); i++) {
                TagColorOption option = colors.get(i);
                if (option == null) {
                    continue;
                }

                TextComponent colorComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', option.colorCode + option.name));
                colorComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan cortag " + option.name));
                colorComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clique para usar a cor " + option.name).create()));
                line.addExtra(colorComponent);

                if (i < colors.size() - 1) {
                    line.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&f, ")));
                }

                if ((i + 1) % 4 == 0) {
                    p.spigot().sendMessage(line);
                    line = new TextComponent("");
                }
            }

            if (line.getExtra() != null && !line.getExtra().isEmpty()) {
                p.spigot().sendMessage(line);
            }

            return true;
        }

        String colorName = args[1];
        TagColorOption selected = null;

        for (TagColorOption option : getSortedTagColors()) {
            if (option == null) {
                continue;
            }

            if (ChatColor.stripColor(option.name).toLowerCase(Locale.ROOT).contains(colorName.toLowerCase(Locale.ROOT))) {
                selected = option;
                break;
            }
        }

        if (selected == null) {
            p.sendMessage("§cCor não encontrada!");
            return true;
        }

        Managers.getClanManager().setTagColor(clan, selected.colorCode);
        p.sendMessage("§aCor da tag alterada para: " + selected.colorCode + selected.name);
        return true;
    }

    private List<TagColorOption> getSortedTagColors() {
        List<TagColorOption> colors = new ArrayList<>();

        for (String raw : Managers.getClanManager().getTagColors()) {
            TagColorOption option = parseTagColor(raw);
            if (option != null) {
                colors.add(option);
            }
        }

        colors.sort(Comparator.comparing(option -> ChatColor.stripColor(option.name).toLowerCase(Locale.ROOT)));
        return colors;
    }

    private TagColorOption parseTagColor(String raw) {
        if (raw == null || raw.length() < 3 || raw.charAt(0) != '&') {
            return null;
        }

        String colorCode = raw.substring(0, 2);
        String name = raw.substring(2).trim();
        if (name.isEmpty()) {
            return null;
        }

        return new TagColorOption(colorCode, name);
    }

    private static class TagColorOption {
        private final String colorCode;
        private final String name;

        private TagColorOption(String colorCode, String name) {
            this.colorCode = colorCode == null ? "" : colorCode;
            this.name = name == null ? "" : name;
        }
    }

    private boolean cmdEditTag(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan editar <nova_tag>");
            return true;
        }

        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }

        if (!clan.isLeader(p.getUniqueId())) {
            p.sendMessage("§cApenas o líder pode editar a tag!");
            return true;
        }

        String newTag = args[1].toUpperCase();
        if (newTag.length() > 6) {
            p.sendMessage("§cA tag deve ter no máximo 6 caracteres!");
            return true;
        }

        clan.setTag(newTag);
        Managers.getClanManager().saveClan(clan);
        p.sendMessage("§aTag alterada para: " + clan.getFormattedTag());
        return true;
    }

    private boolean cmdInfo(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan ver <tag>");
            return true;
        }

        String clanName = args[1];
        Clan clan = Managers.getClanManager().getClan(clanName);

        if (clan == null) {
            p.sendMessage("§cClan não encontrado!");
            return true;
        }

        p.sendMessage("§5§m+-------------------§5[ §d" + clan.getName() + " §5]§m-------------------+");
        p.sendMessage(" §7Tag: " + clan.getFormattedTag());
        p.sendMessage(" §7Líder: §f" + Bukkit.getOfflinePlayer(clan.getLeader()).getName());
        p.sendMessage(" §7Membros: §f" + clan.getMemberCount() + "/" + clan.getMaxMembers());
        p.sendMessage(" §7Criado em: §f" + new Date(clan.getCreationDate()).toString());
        p.sendMessage(" §7Wins: §f" + clan.getWins());
        p.sendMessage(" §7Losses: §f" + clan.getLosses());
        p.sendMessage(" §7Elo: §f" + clan.getElo());
        p.sendMessage("§5§m----------------------------------------------------------+");
        return true;
    }

    private boolean cmdMembers(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUse: /clan membros <tag>");
            return true;
        }

        String clanName = args[1];
        Clan clan = Managers.getClanManager().getClan(clanName);

        if (clan == null) {
            p.sendMessage("§cClan não encontrado!");
            return true;
        }

        p.sendMessage("§5§m+-------------------§5[ §dMembros de " + clan.getName() + " §5]§m-------------------+");

        for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            String roleDisplay = entry.getValue().getDisplay();
            p.sendMessage(" §7- " + roleDisplay + " §f" + playerName);
        }

        p.sendMessage("§5§m----------------------------------------------------------+");
        return true;
    }

    private boolean cmdList(Player p, String[] args) {
        Collection<Clan> clans = Managers.getClanManager().getAllClans();

        if (clans.isEmpty()) {
            p.sendMessage("§cNão há clans criados.");
            return true;
        }

        p.sendMessage("§5§m+-------------------§5[ §dClans §5]§m-------------------+");

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {}
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, clans.size());

        List<Clan> clanList = new ArrayList<>(clans);
        for (int i = start; i < end; i++) {
            Clan clan = clanList.get(i);
            p.sendMessage(" §7" + (i + 1) + ". " + clan.getFormattedTag() + " §f- " + clan.getName());
        }

        p.sendMessage("§5§m----------------------------------------------------------+");
        p.sendMessage("§7Página §f" + page + " §7de §f" + ((clans.size() + 9) / 10));
        return true;
    }

    private boolean cmdChat(Player p, String[] args) {
        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null) {
            p.sendMessage("§cVocê não está em um clan!");
            return true;
        }
        p.sendMessage("§aVocê entrou no chat do clan!");
        return true;
    }

    private String extractColorName(String value) {
        String[] split = value.split("(?<=&[0-9a-f])");
        return split.length >= 2 ? split[1] : "";
    }
}
