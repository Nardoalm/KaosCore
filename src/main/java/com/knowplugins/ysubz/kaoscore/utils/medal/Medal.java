package com.knowplugins.ysubz.kaoscore.utils.medal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Medal {

    private final String id;
    private final String name;
    private final String permission;
    private final List<String> chatFrames;
    private final List<String> nametagFrames;
    private final int intervalTicks;

    public Medal(String id, String name, String permission, List<String> chatFrames, List<String> nametagFrames, int intervalTicks) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.permission = permission == null ? "" : permission;
        this.chatFrames = sanitize(chatFrames);
        this.nametagFrames = sanitize(nametagFrames);
        this.intervalTicks = Math.max(10, intervalTicks);
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

    public List<String> getChatFrames() {
        return chatFrames;
    }

    public List<String> getNametagFrames() {
        return nametagFrames;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    public boolean isAnimated() {
        return chatFrames.size() > 1 || nametagFrames.size() > 1;
    }

    public String getStaticChat() {
        return chatFrames.isEmpty() ? "" : chatFrames.get(0);
    }

    public String getStaticNametag() {
        return nametagFrames.isEmpty() ? "" : nametagFrames.get(0);
    }

    public String getChatFrame(int index) {
        return frame(chatFrames, index);
    }

    public String getNametagFrame(int index) {
        return frame(nametagFrames, index);
    }

    private List<String> sanitize(List<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        for (String value : source) {
            if (value != null) {
                list.add(value);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private String frame(List<String> frames, int index) {
        if (frames.isEmpty()) {
            return "";
        }
        if (frames.size() == 1) {
            return frames.get(0);
        }
        int safeIndex = Math.abs(index) % frames.size();
        return frames.get(safeIndex);
    }
}
