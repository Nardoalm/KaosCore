package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.permission.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GroupCommand extends Commands {

    private static final String PERMISSION = "kaoscore.admin";

    public GroupCommand() {
        super("group");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!hasPerm(sender)) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PermissionManager permissions = Managers.getPermissionManager();
        if (permissions == null) {
            sender.sendMessage("§cPermission system is currently unavailable.");
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
                sender.sendMessage("§aGroups: §f" + (names.isEmpty() ? "(none)" : String.join(", ", names)));
                return;
            }
            case "info": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /grant info <group>");
                    return;
                }
                PermissionGroup group = permissions.getGroup(args[1]);
                if (group == null) {
                    sender.sendMessage("§cGroup not found.");
                    return;
                }

                sender.sendMessage("§aGRANT INFO");
                sender.sendMessage(" §7Name: §f" + group.getName());
                sender.sendMessage(" §7Priority: §f" + group.getPriority());
                sender.sendMessage(" §7Default: §f" + (group.isDefaultGroup() ? "yes" : "no"));
                sender.sendMessage(" §7Prefix: §f" + group.getPrefix());
                sender.sendMessage(" §7Suffix: §f" + group.getSuffix());
                sender.sendMessage(" §7Inherits: §f" + (group.getInherits().isEmpty() ? "(none)" : String.join(", ", group.getInherits())));
                sender.sendMessage(" §7Permissions: §f" + group.getPermissions().size());
                return;
            }
            case "create": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /grant create <name> [default]");
                    return;
                }
                String name = args[1];
                boolean isDefault = args.length >= 3 && args[2].equalsIgnoreCase("default");

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.createGroup(name, isDefault);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ok
                                ? "§aGroup created: §f" + name + (isDefault ? " §7(default)" : "")
                                : "§cFailed to create group.");
                    });
                });
                return;
            }
            case "delete":
            case "remove": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /grant delete <name>");
                    return;
                }
                String name = args[1];

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.deleteGroup(name);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§aGroup removed: §f" + name
                            : "§cFailed to remove group."));
                });
                return;
            }
            case "default": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /grant default <group>");
                    return;
                }
                String name = args[1];
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setDefaultGroup(name);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§aDefault group is now: §f" + name
                            : "§cFailed to set default group."));
                });
                return;
            }
            case "setpriority": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /grant setpriority <group> <priority>");
                    return;
                }
                String name = args[1];
                int priority;
                try {
                    priority = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage("§cInvalid priority.");
                    return;
                }

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setGroupPriority(name, priority);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§aPriority updated."
                            : "§cFailed to update priority."));
                });
                return;
            }
            case "setprefix": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /grant setprefix <group> <prefix>");
                    return;
                }
                String name = args[1];
                String prefix = join(args, 2);
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setGroupPrefix(name, prefix);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§aPrefix updated."
                            : "§cFailed to update prefix."));
                });
                return;
            }
            case "setsuffix": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /grant setsuffix <group> <suffix>");
                    return;
                }
                String name = args[1];
                String suffix = join(args, 2);
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.setGroupSuffix(name, suffix);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§aSuffix updated."
                            : "§cFailed to update suffix."));
                });
                return;
            }
            case "addperm": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /grant addperm <group> <permission> [true|false]");
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
                            ? "§aPermission added."
                            : "§cFailed to add permission."));
                });
                return;
            }
            case "delperm": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /grant delperm <group> <permission>");
                    return;
                }
                String group = args[1];
                String perm = args[2];

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = permissions.removeGroupPermission(group, perm);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ok
                            ? "§aPermission removed."
                            : "§cFailed to remove permission."));
                });
                return;
            }
            case "inherit": {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /grant inherit add|remove <group> <parent>");
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
                            ? "§aInheritance updated."
                            : "§cFailed to update inheritance."));
                });
                return;
            }
            case "reload": {
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GroupCommand.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    permissions.reloadGroups();
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§aGroups reloaded."));
                });
                return;
            }
            default:
                sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> lines = new ArrayList<>();
        lines.add("§eAvailable Commands:");
        lines.add("§e /grant list");
        lines.add("§e /grant info <group>");
        lines.add("§e /grant create <group> [default]");
        lines.add("§e /grant delete <group>");
        lines.add("§e /grant default <group>");
        lines.add("§e /grant setpriority <group> <priority>");
        lines.add("§e /grant setprefix <group> <prefix>");
        lines.add("§e /grant setsuffix <group> <suffix>");
        lines.add("§e /grant addperm <group> <perm> [true|false]");
        lines.add("§e /grant delperm <group> <perm>");
        lines.add("§e /grant inherit add|remove <group> <parent>");
        lines.add("§e /grant reload");
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