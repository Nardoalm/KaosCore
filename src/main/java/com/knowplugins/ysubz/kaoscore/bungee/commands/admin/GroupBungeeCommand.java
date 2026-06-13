package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.services.BungeePermissionService;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class GroupBungeeCommand extends BaseBungeeCommand {

    GroupBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "group", "grupo", "rank");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.permissions", "§c§lGROUP ➔ §cVocê não possui permissão para executar este comando.")) {
            return;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        BungeePermissionService permissions = plugin.getPermissions();
        String sub = Texts.normalize(args[0]);
        if ("list".equals(sub)) {
            Set<String> names = permissions.getGroupNames();
            reply(sender, "§a§lGROUP ➔ §aGrupos: §f" + (names.isEmpty() ? "(nenhum)" : String.join(", ", names)));
            return;
        }
        if ("info".equals(sub)) {
            if (args.length < 2) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group info <grupo>");
                return;
            }
            PermissionGroup group = permissions.getGroup(args[1]);
            if (group == null) {
                reply(sender, "§c§lGROUP ➔ §cGrupo não encontrado.");
                return;
            }
            reply(sender, "§a§lGROUP INFO");
            reply(sender, " §7Nome: §f" + group.getName());
            reply(sender, " §7Prioridade: §f" + group.getPriority());
            reply(sender, " §7Default: §f" + (group.isDefaultGroup() ? "sim" : "não"));
            reply(sender, " §7Prefix: §f" + group.getPrefix());
            reply(sender, " §7Suffix: §f" + group.getSuffix());
            reply(sender, " §7Herda: §f" + (group.getInherits().isEmpty() ? "(nenhum)" : String.join(", ", group.getInherits())));
            reply(sender, " §7Permissões: §f" + group.getPermissions().size());
            return;
        }
        if ("create".equals(sub)) {
            if (args.length < 2) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group create <nome> [default]");
                return;
            }
            boolean result = permissions.createGroup(args[1], args.length >= 3 && "default".equalsIgnoreCase(args[2]));
            reply(sender, result ? "§a§lGROUP ➔ §aGrupo criado: §f" + args[1] : "§c§lGROUP ➔ §cFalha ao criar grupo.");
            return;
        }
        if ("delete".equals(sub) || "remove".equals(sub)) {
            if (args.length < 2) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group delete <nome>");
                return;
            }
            boolean result = permissions.deleteGroup(args[1]);
            reply(sender, result ? "§a§lGROUP ➔ §aGrupo removido: §f" + args[1] : "§c§lGROUP ➔ §cFalha ao remover grupo.");
            return;
        }
        if ("default".equals(sub)) {
            if (args.length < 2) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group default <grupo>");
                return;
            }
            reply(sender, permissions.setDefaultGroup(args[1]) ? "§a§lGROUP ➔ §aGrupo default atualizado." : "§c§lGROUP ➔ §cFalha ao setar default.");
            return;
        }
        if ("setpriority".equals(sub)) {
            if (args.length < 3) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group setpriority <grupo> <prioridade>");
                return;
            }
            try {
                int priority = Integer.parseInt(args[2]);
                reply(sender, permissions.setGroupPriority(args[1], priority) ? "§a§lGROUP ➔ §aPrioridade atualizada." : "§c§lGROUP ➔ §cFalha ao atualizar prioridade.");
            } catch (NumberFormatException exception) {
                reply(sender, "§c§lGROUP ➔ §cPrioridade inválida.");
            }
            return;
        }
        if ("setprefix".equals(sub)) {
            if (args.length < 3) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group setprefix <grupo> <prefix>");
                return;
            }
            reply(sender, permissions.setGroupPrefix(args[1], Texts.join(args, 2)) ? "§a§lGROUP ➔ §aPrefix atualizado." : "§c§lGROUP ➔ §cFalha ao atualizar prefix.");
            return;
        }
        if ("setsuffix".equals(sub)) {
            if (args.length < 3) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group setsuffix <grupo> <suffix>");
                return;
            }
            reply(sender, permissions.setGroupSuffix(args[1], Texts.join(args, 2)) ? "§a§lGROUP ➔ §aSuffix atualizado." : "§c§lGROUP ➔ §cFalha ao atualizar suffix.");
            return;
        }
        if ("addperm".equals(sub)) {
            if (args.length < 3) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group addperm <grupo> <permissão> [true|false]");
                return;
            }
            String permission = args[2];
            boolean value = true;
            if (permission.startsWith("-")) {
                permission = permission.substring(1);
                value = false;
            }
            if (args.length >= 4) {
                value = Boolean.parseBoolean(args[3]);
            }
            reply(sender, permissions.addGroupPermission(args[1], permission, value) ? "§a§lGROUP ➔ §aPerm adicionada." : "§c§lGROUP ➔ §cFalha ao adicionar perm.");
            return;
        }
        if ("delperm".equals(sub)) {
            if (args.length < 3) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group delperm <grupo> <permissão>");
                return;
            }
            reply(sender, permissions.removeGroupPermission(args[1], args[2]) ? "§a§lGROUP ➔ §aPerm removida." : "§c§lGROUP ➔ §cFalha ao remover perm.");
            return;
        }
        if ("inherit".equals(sub)) {
            if (args.length < 4) {
                reply(sender, "§c§lGROUP ➔ §cUso: /group inherit add|remove <grupo> <pai>");
                return;
            }
            boolean result;
            if ("add".equalsIgnoreCase(args[1])) {
                result = permissions.addGroupInherit(args[2], args[3]);
            } else if ("remove".equalsIgnoreCase(args[1]) || "del".equalsIgnoreCase(args[1])) {
                result = permissions.removeGroupInherit(args[2], args[3]);
            } else {
                result = false;
            }
            reply(sender, result ? "§a§lGROUP ➔ §aHerança atualizada." : "§c§lGROUP ➔ §cFalha ao atualizar herança.");
            return;
        }
        if ("reload".equals(sub)) {
            permissions.reload(plugin.getPermissionsConfig());
            reply(sender, "§a§lGROUP ➔ §aGrupos recarregados.");
            return;
        }

        sendHelp(sender);
    }

    private void sendHelp(CommandSender sender) {
        reply(sender, "§e§lGROUP ➔ §eComandos:");
        reply(sender, "§e• §e/group list");
        reply(sender, "§e• §e/group info <grupo>");
        reply(sender, "§e• §e/group create <grupo> [default]");
        reply(sender, "§e• §e/group delete <grupo>");
        reply(sender, "§e• §e/group default <grupo>");
        reply(sender, "§e• §e/group setpriority <grupo> <prioridade>");
        reply(sender, "§e• §e/group setprefix <grupo> <prefix>");
        reply(sender, "§e• §e/group setsuffix <grupo> <suffix>");
        reply(sender, "§e• §e/group addperm <grupo> <perm> [true|false]");
        reply(sender, "§e• §e/group delperm <grupo> <perm>");
        reply(sender, "§e• §e/group inherit add|remove <grupo> <pai>");
        reply(sender, "§e• §e/group reload");
    }
}
