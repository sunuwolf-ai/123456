package com.jjajang.rpg.jjajjang;

import com.jjajang.rpg.util.WeaponUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class JjajjangItems {

    public static ItemStack createT1(JavaPlugin plugin) {
        return make(plugin, Material.BOWL, ChatColor.GOLD + "🥣 짜장 그릇", 1,
                ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "짜장소환  " + ChatColor.DARK_GRAY + "쿨 7초",
                ChatColor.GRAY + "  대상 머리 위에 짜장을 소환해 떨어뜨린다");
    }

    public static ItemStack createT2(JavaPlugin plugin) {
        return make(plugin, Material.HONEY_BOTTLE, ChatColor.DARK_PURPLE + "🥣 명품 레스토랑", 2,
                ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "짜장면 투척  " + ChatColor.DARK_GRAY + "쿨 10초",
                ChatColor.GRAY + "  춘장이 묻어 지속 피해를 입힌다");
    }

    public static ItemStack createT3(JavaPlugin plugin) {
        return make(plugin, Material.RABBIT_STEW, ChatColor.DARK_RED + "🥣 중국집 코스요리", 3,
                ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                ChatColor.YELLOW + "[우클릭] " + ChatColor.WHITE + "준우준우화  " + ChatColor.DARK_GRAY + "쿨 20초",
                ChatColor.GRAY + "  주변 적을 살찌워 독+이속감소, 갑옷 한 피스 탈락");
    }

    private static ItemStack make(JavaPlugin plugin, Material mat, String name, int tier, String... skills) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "[짜짱 " + tier + "차 무기]");
        lore.add("");
        for (String s : skills) lore.add(s);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "※ 패시브: 스킬 명중 시 짜장 그릇 드랍, 회수 시 체력회복");
        lore.add(ChatColor.DARK_GRAY + "※ 타 직업 사용 시 데미지 50% 감소");
        meta.setLore(lore);
        WeaponUtils.tagWeapon(meta, plugin, "jjajjang", tier);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isJjajjangWeapon(ItemStack item, JavaPlugin plugin) {
        return "jjajjang".equals(WeaponUtils.getWeaponClass(item, plugin));
    }
}
