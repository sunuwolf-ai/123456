package com.jjajang.rpg.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CashVoucherItem {
    private static NamespacedKey KEY;

    public static void init(JavaPlugin plugin) {
        KEY = new NamespacedKey(plugin, "cash_voucher");
    }

    public static ItemStack create(long cashAmount) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✦ " + String.format("%,d", cashAmount) + " 캐시 교환권 ✦");
        meta.setCustomModelData(1007);
        meta.setLore(List.of(
            ChatColor.GRAY + "우클릭하면 " + String.format("%,d", cashAmount) + " 캐시를 지급받습니다.",
            ChatColor.DARK_GRAY + "※ 1회용 교환권"
        ));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.LONG, cashAmount);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean is(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.LONG);
    }

    public static long getCashAmount(ItemStack item) {
        if (!is(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(KEY, PersistentDataType.LONG, 0L);
    }
}
