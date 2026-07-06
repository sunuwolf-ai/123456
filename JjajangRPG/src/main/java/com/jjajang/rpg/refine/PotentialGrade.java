package com.jjajang.rpg.refine;

import org.bukkit.ChatColor;

public enum PotentialGrade {
    EPIC    ("에픽",    ChatColor.DARK_PURPLE, 0),
    UNIQUE  ("유니크",  ChatColor.YELLOW,      1),
    LEGENDARY("레전드리", ChatColor.LIGHT_PURPLE, 2);

    public final String displayName;
    public final ChatColor color;
    public final int level; // 0=epic,1=unique,2=legendary

    PotentialGrade(String displayName, ChatColor color, int level) {
        this.displayName = displayName;
        this.color = color;
        this.level = level;
    }

    public PotentialGrade next() {
        return switch (this) {
            case EPIC -> UNIQUE;
            case UNIQUE -> LEGENDARY;
            case LEGENDARY -> LEGENDARY;
        };
    }

    public boolean isMax() { return this == LEGENDARY; }
}
