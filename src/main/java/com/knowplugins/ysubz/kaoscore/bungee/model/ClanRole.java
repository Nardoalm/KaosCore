package com.knowplugins.ysubz.kaoscore.bungee.model;

public enum ClanRole {
    LEADER("§4§lLIDER", 100),
    MOD("§c§lMOD", 80),
    MEMBER("§7Membro", 60),
    RECRUIT("§8Recruta", 40);

    private final String display;
    private final int power;

    ClanRole(String display, int power) {
        this.display = display;
        this.power = power;
    }

    public String getDisplay() {
        return display;
    }

    public boolean canKick(ClanRole target) {
        return target != null && power > target.power;
    }

    public static ClanRole getByName(String name) {
        for (ClanRole role : values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return MEMBER;
    }
}
