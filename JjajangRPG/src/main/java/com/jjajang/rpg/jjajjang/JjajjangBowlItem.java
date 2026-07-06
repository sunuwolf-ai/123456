package com.jjajang.rpg.jjajjang;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/** 짜짱 패시브: 스킬 명중 시 떨어지는 회수용 짜장 그릇 (스킬을 쓴 본인만 회수 가능) */
public class JjajjangBowlItem {

    private static NamespacedKey KEY;
    private static NamespacedKey KEY_OWNER;

    public static void init(JavaPlugin plugin) {
        KEY       = new NamespacedKey(plugin, "jjajjang_bowl");
        KEY_OWNER = new NamespacedKey(plugin, "jjajjang_bowl_owner");
    }

    public static ItemStack create(UUID owner) {
        ItemStack item = new ItemStack(Material.BOWL, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "🥣 떨어진 짜장 그릇");
        meta.setLore(List.of(ChatColor.GRAY + "본인만 회수해 체력을 회복할 수 있습니다."));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(KEY_OWNER, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return item;
    }

    public static boolean is(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    public static boolean isOwner(ItemStack item, UUID player) {
        if (!is(item)) return false;
        String owner = item.getItemMeta().getPersistentDataContainer().get(KEY_OWNER, PersistentDataType.STRING);
        return owner != null && owner.equals(player.toString());
    }
}
