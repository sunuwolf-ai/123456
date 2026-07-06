package com.jjajang.rpg.rogue;

import com.jjajang.rpg.util.WeaponUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class RogueItems {

    public static ItemStack createT1(JavaPlugin plugin) {
        return make(plugin, Material.WOODEN_SWORD, ChatColor.DARK_PURPLE + "🗡 암살자의 단검", 1,
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "표창 투척  " + ChatColor.DARK_GRAY + "쿨 3초",
                ChatColor.YELLOW + "[패시브] " + ChatColor.WHITE + "기습",
                ChatColor.GRAY + "  적 뒤에서 공격 시 크리티컬 2배 데미지");
    }

    public static ItemStack createT2(JavaPlugin plugin) {
        return make(plugin, Material.STONE_SWORD, ChatColor.DARK_PURPLE + "🗡 혈독 단검", 2,
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "출혈 기습  " + ChatColor.DARK_GRAY + "쿨 15초",
                ChatColor.GRAY + "  투척으로 출혈 + 잠시 은신");
    }

    public static ItemStack createT3(JavaPlugin plugin) {
        return make(plugin, Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "🗡 망령의 단검", 3,
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "연막  " + ChatColor.DARK_GRAY + "쿨 60초",
                ChatColor.GRAY + "  연막 생성 + 이동속도↑ + 공격속도↑ (10초)");
    }

    private static ItemStack make(JavaPlugin plugin, Material mat, String name, int tier, String... skills) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "[도적 " + tier + "차 무기]");
        lore.add("");
        for (String s : skills) lore.add(s);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "※ 타 직업 사용 시 데미지 50% 감소");
        meta.setLore(lore);
        WeaponUtils.tagWeapon(meta, plugin, "rogue", tier);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRogueWeapon(ItemStack item, JavaPlugin plugin) {
        return "rogue".equals(WeaponUtils.getWeaponClass(item, plugin));
    }
}
