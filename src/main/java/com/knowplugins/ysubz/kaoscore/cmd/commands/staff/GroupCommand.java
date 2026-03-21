package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GroupCommand extends Commands {

    private static final String PERMISSION = "kaoscore.staff.permissions";

    public GroupCommand() {
        super("group", "grupo", "rank");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!hasPerm(sender)) {
            sender.sendMessage("§c§lGROUP ➔ §cVocê não possui permissão para executar este comando.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PermissionManager permissions = Managers.getPermissionManager();
        if (permissions == null) {
            sender.sendMessage("§c§lGROUP ➔ §cSistema de permissões indisponível.");
            return;
        }

        if (args == null || args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list": {
                Set<String> names = permissions.getGroupNames();
                sender.sendMessage("§a§lGROUP ➔ §aGrupos: §f" + (names.isEmpty() ? "(nenhum)" : String.join(", ", names)));
                return;
            }
            case "info": {
                if (args.length < 2) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group info <grupo>");
                    return;
                }
                PermissionGroup group = permissions.getGroup(args[1]);
                if (group == null) {
                    sender.sendMessage("§c§lGROUP ➔ §cGrupo não encontrado.");
                    return;
                }

                sender.sendMessage("§a§lGROUP INFO");
                sender.sendMessage(" §7Nome: §f" + group.getName());
                sender.sendMessage(" §7Prioridade: §f" + group.getPriority());
                sender.sendMessage(" §7Default: §f" + (group.isDefaultGroup() ? "sim" : "não"));
                sender.sendMessage(" §7Prefix: §f" + group.getPrefix());
                sender.sendMessage(" §7Suffix: §f" + group.getSuffix());
                sender.sendMessage(" §7Herda: §f" + (group.getInherits().isEmpty() ? "(nenhum)" : String.join(", ", group.getInherits())));
                sender.sendMessage(" §7Permissões: §f" + group.getPermissions().size());
                return;
            }
            case "create": {
                if (args.length < 2) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group create <nome> [default]");
                    return;
                }
                String name = args[1];
                boolean isDefault = args.length >= 3 && args[2].equalsIgnoreCase("default");

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.createGroup(name, isDefault);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ok
                                ? "§a§lGROUP ➔ §aGrupo criado: §f" + name + (isDefault ? " §7(default)" : "")
                                : "§c§lGROUP ➔ §cFalha ao criar grupo.");
                    });
                });
                return;
            }
            case "delete":
            case "remove": {
                if (args.length < 2) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group delete <nome>");
                    return;
                }
                String name = args[1];

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.deleteGroup(name);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aGrupo removido: §f" + name
                            : "§c§lGROUP ➔ §cFalha ao remover grupo."));
                });
                return;
            }
            case "default": {
                if (args.length < 2) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group default <grupo>");
                    return;
                }
                String name = args[1];
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setDefaultGroup(name);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aGrupo default agora é: §f" + name
                            : "§c§lGROUP ➔ §cFalha ao setar default."));
                });
                return;
            }
            case "setpriority": {
                if (args.length < 3) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group setpriority <grupo> <prioridade>");
                    return;
                }
                String name = args[1];
                int priority;
                try {
                    priority = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage("§c§lGROUP ➔ §cPrioridade inválida.");
                    return;
                }

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setGroupPriority(name, priority);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aPrioridade atualizada."
                            : "§c§lGROUP ➔ §cFalha ao atualizar prioridade."));
                });
                return;
            }
            case "setprefix": {
                if (args.length < 3) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group setprefix <grupo> <prefix>");
                    return;
                }
                String name = args[1];
                String prefix = join(args, 2);
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setGroupPrefix(name, prefix);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aPrefix atualizado."
                            : "§c§lGROUP ➔ §cFalha ao atualizar prefix."));
                });
                return;
            }
            case "setsuffix": {
                if (args.length < 3) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group setsuffix <grupo> <suffix>");
                    return;
                }
                String name = args[1];
                String suffix = join(args, 2);
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setGroupSuffix(name, suffix);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aSuffix atualizado."
                            : "§c§lGROUP ➔ §cFalha ao atualizar suffix."));
                });
                return;
            }
            case "addperm": {
                if (args.length < 3) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group addperm <grupo> <permissão> [true|false]");
                    return;
                }
                String group = args[1];
                String perm = args[2];
                boolean value = true;

                if (perm.startsWith("-")) {
                    value = false;
                    perm = perm.substring(1);
                }

                if (args.length >= 4) {
                    value = Boolean.parseBoolean(args[3]);
                }

                String finalPerm = perm;
                boolean finalValue = value;
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.addGroupPermission(group, finalPerm, finalValue);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aPerm adicionada."
                            : "§c§lGROUP ➔ §cFalha ao adicionar perm."));
                });
                return;
            }
            case "delperm": {
                if (args.length < 3) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group delperm <grupo> <permissão>");
                    return;
                }
                String group = args[1];
                String perm = args[2];

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.removeGroupPermission(group, perm);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§a§lGROUP ➔ §aPerm removida."
                            : "§c§lGROUP ➔ §cFalha ao remover perm."));
                });
                return;
            }
            case "inherit": {
                if (args.length < 4) {
                    sender.sendMessage("§c§lGROUP ➔ §cUso: /group inherit add|remove <grupo> <pai>");
                    return;
                }
                String action = args[1].toLowerCase();
                String group = args[2];
                String parent = args[3];

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok;
                    if (action.equals("add")) {
                        ok = permissions.addGroupInherit(group, parent);
                    } else if (action.equals("remove") || action.equals("del")) {
                        ok = permissions.removeGroupInherit(group, parent);
                    } else {
                        ok = false;
                    }

                    boolean finalOk = ok;
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(finalOk
                            ? "§a§lGROUP ➔ §aHerança atualizada."
                            : "§c§lGROUP ➔ §cFalha ao atualizar herança."));
                });
                return;
            }
            case "reload": {
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    permissions.reloadGroups();
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§a§lGROUP ➔ §aGrupos recarregados."));
                });
                return;
            }
            default:
                sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> lines = new ArrayList<>();
        lines.add("§e§lGROUP ➔ §eComandos:");
        lines.add("§e• §e/group list");
        lines.add("§e• §e/group info <grupo>");
        lines.add("§e• §e/group create <grupo> [default]");
        lines.add("§e• §e/group delete <grupo>");
        lines.add("§e• §e/group default <grupo>");
        lines.add("§e• §e/group setpriority <grupo> <prioridade>");
        lines.add("§e• §e/group setprefix <grupo> <prefix>");
        lines.add("§e• §e/group setsuffix <grupo> <suffix>");
        lines.add("§e• §e/group addperm <grupo> <perm> [true|false]");
        lines.add("§e• §e/group delperm <grupo> <perm>");
        lines.add("§e• §e/group inherit add|remove <grupo> <pai>");
        lines.add("§e• §e/group reload");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    private boolean hasPerm(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.hasPermission(PERMISSION);
    }

    private String join(String[] args, int start) {
        if (args == null || start < 0 || start >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}

