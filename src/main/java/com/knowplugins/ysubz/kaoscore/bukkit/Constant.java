package com.knowplugins.ysubz.kaoscore.bukkit;

public enum Constant {
    NO_PERMISSION_COMMAND("§cYou do not have permission to use this command."),
    NO_PERMISSION("§cYou do not have permission.");

    public final String message;

    Constant(String message) {
        this.message = message;
    }
}
