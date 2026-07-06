package com.jjajang.rpg.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ExpCouponItem {

    private static NamespacedKey KEY_MULTIPLIER;

    public static void init(JavaPlugin plugin) {
        KEY_MULTIPLIER = new NamespacedKey(plugin, "exp_coupon_multiplier");
    }

    /** multiplier: 2 또는 3. 30분 지속. */
    public static ItemStack create(int multiplier) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        if (multiplier == 2) {
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "★ 경험치 2배 쿠폰");
            meta.setCustomModelData(1009);
        } else {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "★ 경험치 3배 쿠폰");
            meta.setCustomModelData(1010);
        }
        meta.setLore(List.of(
                ChatColor.GRAY + "우클릭으로 사용",
                ChatColor.YELLOW + "경험치 " + multiplier + "배 (30분)",
                ChatColor.DARK_GRAY + "사용 시 기존 쿠폰과 중복되지 않습니다"
        ));
        meta.getPersistentDataContainer().set(KEY_MULTIPLIER, PersistentDataType.INTEGER, multiplier);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean is(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_MULTIPLIER, PersistentDataType.INTEGER);
    }

    public static int getMultiplier(ItemStack item) {
        if (!is(item)) return 1;
        Integer val = item.getItemMeta().getPersistentDataContainer().get(KEY_MULTIPLIER, PersistentDataType.INTEGER);
        return val != null ? val : 1;
    }
}
