package com.knowplugins.ysubz.kaoscore.utils.medal;

import java.util.List;

public class Medal {

    private final String id;
    private final String name;
    private final String permission;
    private final String staticChat;
    private final String staticNametag;

    public Medal(String id, String name, String permission, List<String> chatFrames, List<String> nametagFrames) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.permission = permission == null ? "" : permission;
        this.staticChat = first(chatFrames);
        this.staticNametag = first(nametagFrames);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getStaticChat() {
        return staticChat;
    }

    public String getStaticNametag() {
        return staticNametag;
    }

    private String first(List<String> source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        for (String value : source) {
            if (value != null) {
                return value;
            }
        }
        return "";
    }
}
