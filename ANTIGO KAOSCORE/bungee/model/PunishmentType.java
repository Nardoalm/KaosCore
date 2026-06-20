package com.knowplugins.ysubz.kaoscore.bungee;

public enum PunishmentType {
    BAN("banimento", "banido"),
    MUTE("silenciamento", "mutado"),
    WARN("advertencia", "advertido");

    private final String displayName;
    private final String pastName;

    PunishmentType(String displayName, String pastName) {
        this.displayName = displayName;
        this.pastName = pastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPastName() {
        return pastName;
    }
}


