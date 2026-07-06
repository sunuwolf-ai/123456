package com.jjajang.rpg.warrior;

import com.jjajang.rpg.util.WeaponUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WarriorItems {

    public static ItemStack createT1(JavaPlugin plugin) {
        return make(plugin, Material.STONE_SWORD, ChatColor.RED + "⚔ 전사의 석검",
                "[전사 1차 무기]",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "전방 베기  " + ChatColor.DARK_GRAY + "쿨 22초",
                ChatColor.GRAY + "  좁은 전방 범위를 베어 단일 피해를 준다.");
    }

    public static ItemStack createT2(JavaPlugin plugin) {
        return make(plugin, Material.IRON_SWORD, ChatColor.RED + "⚔ 전사의 검",
                "[전사 2차 무기]",
                ChatColor.YELLOW + "[우클릭 1타] " + ChatColor.WHITE + "검찌르기  " + ChatColor.DARK_GRAY + "쿨 20초",
                ChatColor.YELLOW + "[5초 내 2타] " + ChatColor.WHITE + "회전베기");
    }

    public static ItemStack createT3(JavaPlugin plugin) {
        return make(plugin, Material.DIAMOND_SWORD, ChatColor.DARK_RED + "⚔ 전사의 대검",
                "[전사 3차 무기]",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "불굴의 의지  " + ChatColor.DARK_GRAY + "쿨 90초",
                ChatColor.GRAY + "  체력을 최대로 회복하고 주변 적을 1초 기절");
    }

    private static ItemStack make(JavaPlugin plugin, Material mat, String name, String classTag, String... skills) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + classTag);
        lore.add("");
        for (String s : skills) lore.add(s);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "※ 타 직업 사용 시 데미지 50% 감소");
        meta.setLore(lore);
        int tier = name.contains("석검") ? 1 : name.contains("대검") ? 3 : 2;
        WeaponUtils.tagWeapon(meta, plugin, "warrior", tier);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWarriorWeapon(ItemStack item, JavaPlugin plugin) {
        return "warrior".equals(WeaponUtils.getWeaponClass(item, plugin));
    }
}
