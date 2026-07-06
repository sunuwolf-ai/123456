package com.jjajang.rpg.mage;

import com.jjajang.rpg.util.WeaponUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MageItems {

    public static ItemStack createT1(JavaPlugin plugin) {
        return make(plugin, Material.BOOK, ChatColor.AQUA + "📖 견습 마법서", 1,
                ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "파이어볼  " + ChatColor.DARK_GRAY + "쿨 6초",
                ChatColor.GRAY + "  단일 대상에게 화염 피해");
    }

    public static ItemStack createT2(JavaPlugin plugin) {
        return make(plugin, Material.WRITTEN_BOOK, ChatColor.BLUE + "📖 비전 마법서", 2,
                ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "연쇄 에너지볼  " + ChatColor.DARK_GRAY + "쿨 9초",
                ChatColor.GRAY + "  적중한 적부터 주변으로 최대 5회 추가 전이");
    }

    public static ItemStack createT3(JavaPlugin plugin) {
        return make(plugin, Material.ENCHANTED_BOOK, ChatColor.DARK_BLUE + "📖 대마법사의 마도서", 3,
                ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "멸절의 광선  " + ChatColor.DARK_GRAY + "쿨 16초",
                ChatColor.GRAY + "  보는 방향으로 길고 강력한 광선 발사");
    }

    private static ItemStack make(JavaPlugin plugin, Material mat, String name, int tier, String... skills) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "[마법사 " + tier + "차 무기]");
        lore.add("");
        for (String s : skills) lore.add(s);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "※ 패시브: 적 처치 시 쿨타임 1.5초 감소");
        lore.add(ChatColor.DARK_GRAY + "※ 타 직업 사용 시 데미지 50% 감소");
        meta.setLore(lore);
        WeaponUtils.tagWeapon(meta, plugin, "mage", tier);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isMageWeapon(ItemStack item, JavaPlugin plugin) {
        return "mage".equals(WeaponUtils.getWeaponClass(item, plugin));
    }
}
