package com.knowplugins.nardodev.knowpractice.clan;


public enum ClanRole {

    LEADER("&4&lLIDER", 100),
    MOD("&c&lMOD", 80),
    MEMBER("&7Membro", 60),
    RECRUIT("&8Recruta", 40);

    private String display;
    private int power;

    ClanRole(String display, int power) {
        this.display = display;
        this.power = power;
    }

    public String getDisplay() {
        return display;
    }

    public int getPower() {
        return power;
    }

    public boolean canPromote(ClanRole target) {
        return this.power > target.power;
    }

    public boolean canDemote(ClanRole target) {
        return this.power > target.power;
    }

    public boolean canKick(ClanRole target) {
        return this.power > target.power;
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