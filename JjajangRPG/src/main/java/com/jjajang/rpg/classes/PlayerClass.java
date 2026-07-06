package com.jjajang.rpg.classes;

import org.bukkit.ChatColor;

public enum PlayerClass {
    NONE     ("없음",      ChatColor.GRAY,        0, null),
    WARRIOR_1("전사 1차",  ChatColor.RED,          1, "warrior"),
    WARRIOR_2("전사 2차",  ChatColor.DARK_RED,     2, "warrior"),
    WARRIOR_3("전사 3차",  ChatColor.DARK_RED,     3, "warrior"),
    ARCHER_1 ("궁수 1차",  ChatColor.GREEN,        1, "archer"),
    ARCHER_2 ("궁수 2차",  ChatColor.DARK_GREEN,   2, "archer"),
    ARCHER_3 ("궁수 3차",  ChatColor.DARK_GREEN,   3, "archer"),
    ROGUE_1  ("도적 1차",  ChatColor.DARK_PURPLE,  1, "rogue"),
    ROGUE_2  ("도적 2차",  ChatColor.DARK_PURPLE,  2, "rogue"),
    ROGUE_3  ("도적 3차",  ChatColor.LIGHT_PURPLE, 3, "rogue"),
    MAGE_1   ("마법사 1차", ChatColor.AQUA,         1, "mage"),
    MAGE_2   ("마법사 2차", ChatColor.BLUE,         2, "mage"),
    MAGE_3   ("마법사 3차", ChatColor.DARK_BLUE,    3, "mage"),
    JJAJJANG_1("짜짱 1차",  ChatColor.GOLD,         1, "jjajjang"),
    JJAJJANG_2("짜짱 2차",  ChatColor.DARK_PURPLE,  2, "jjajjang"),
    JJAJJANG_3("짜짱 3차",  ChatColor.DARK_RED,     3, "jjajjang");

    private final String displayName;
    private final ChatColor color;
    private final int tier;
    private final String weaponClass;

    PlayerClass(String displayName, ChatColor color, int tier, String weaponClass) {
        this.displayName = displayName;
        this.color = color;
        this.tier = tier;
        this.weaponClass = weaponClass;
    }

    public String getDisplayName() { return displayName; }
    public ChatColor getColor()    { return color; }
    public String getColored()     { return color + displayName; }
    public int getTier()           { return tier; }
    public String getWeaponClass() { return weaponClass; }

    public boolean isWarrior() { return weaponClass != null && weaponClass.equals("warrior"); }
    public boolean isArcher()  { return weaponClass != null && weaponClass.equals("archer"); }
    public boolean isRogue()   { return weaponClass != null && weaponClass.equals("rogue"); }
    public boolean isMage()    { return weaponClass != null && weaponClass.equals("mage"); }
    public boolean isJjajjang(){ return weaponClass != null && weaponClass.equals("jjajjang"); }
}
