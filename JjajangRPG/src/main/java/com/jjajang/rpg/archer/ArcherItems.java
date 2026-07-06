package com.jjajang.rpg.archer;

import com.jjajang.rpg.util.WeaponUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ArcherItems {

    public static ItemStack createT1(JavaPlugin plugin) {
        return make(plugin, Material.BOW, ChatColor.GREEN + "🏹 사냥꾼의 활", 1,
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "도약 속사  " + ChatColor.DARK_GRAY + "쿨 18초",
                ChatColor.GRAY + "  뒤로 도약하며 화살을 연속 발사");
    }

    public static ItemStack createT2(JavaPlugin plugin) {
        return make(plugin, Material.BOW, ChatColor.GREEN + "🏹 마법사의 활", 2,
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "속성 화살  " + ChatColor.DARK_GRAY + "쿨 6초",
                ChatColor.GRAY + "  빙결→화염→독→번개 순으로 순환 발사");
    }

    public static ItemStack createT3(JavaPlugin plugin) {
        return make(plugin, Material.BOW, ChatColor.DARK_GREEN + "🏹 신궁의 활", 3,
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "화살 비  " + ChatColor.DARK_GRAY + "쿨 90초",
                ChatColor.GRAY + "  광역에 수십 발의 화살이 쏟아진다");
    }

    private static ItemStack make(JavaPlugin plugin, Material mat, String name, int tier, String... skills) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "[궁수 " + tier + "차 무기]");
        lore.add("");
        for (String s : skills) lore.add(s);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "※ 타 직업 사용 시 데미지 50% 감소");
        meta.setLore(lore);
        WeaponUtils.tagWeapon(meta, plugin, "archer", tier);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isArcherWeapon(ItemStack item, JavaPlugin plugin) {
        return "archer".equals(WeaponUtils.getWeaponClass(item, plugin));
    }
}
