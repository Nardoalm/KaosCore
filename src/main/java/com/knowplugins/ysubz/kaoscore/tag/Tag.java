package com.knowplugins.ysubz.kaoscore.tag;

public class Tag {

    private final String id;
    private final String name;
    private final String prefix;
    private final String color;
    private final String suffix;
    private final String permission;
    private final int position;

    public Tag(String id, String name, String prefix, String color, String suffix, String permission, int position) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.prefix = prefix == null ? "" : prefix;
        this.color = color == null ? "" : color;
        this.suffix = suffix == null ? "" : suffix;
        this.permission = permission == null ? "" : permission;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getColor() {
        return color;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getPermission() {
        return permission;
    }

    public int getPosition() {
        return position;
    }
}
