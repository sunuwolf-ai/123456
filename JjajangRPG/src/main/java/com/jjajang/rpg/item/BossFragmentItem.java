package com.jjajang.rpg.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BossFragmentItem {

    private static NamespacedKey KEY;

    public static void init(JavaPlugin plugin) {
        KEY = new NamespacedKey(plugin, "boss_fragment");
    }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "보스의 파편");
        meta.setCustomModelData(1003);
        meta.setLore(List.of(
            ChatColor.GRAY + "강력한 보스로부터 얻은 파편.",
            ChatColor.GRAY + "특수 제작에 사용됩니다.",
            ChatColor.DARK_PURPLE + "★ 희귀 재료"
        ));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBossFragment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(KEY, PersistentDataType.BYTE);
    }
}
