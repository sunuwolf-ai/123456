package com.jjajang.rpg.refine;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class DanmujiItem {

    private static NamespacedKey KEY;

    public static void init(JavaPlugin plugin) {
        KEY = new NamespacedKey(plugin, "danmuji");
    }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.YELLOW_WOOL, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "단무지");
        meta.setCustomModelData(1001);
        meta.setLore(List.of(
            ChatColor.GRAY + "반달 모양의 노란 단무지.",
            ChatColor.GRAY + "세공 시 소모됩니다.",
            ChatColor.DARK_GRAY + "커스텀 세공 재료"
        ));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isDanmuji(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(KEY, PersistentDataType.BYTE);
    }

    /** 인벤토리에서 단무지를 n개 소모. 부족하면 false 반환 */
    public static boolean consume(org.bukkit.entity.Player player, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!isDanmuji(slot)) continue;
            int amt = slot.getAmount();
            if (amt <= remaining) {
                remaining -= amt;
                player.getInventory().setItem(i, null);
            } else {
                slot.setAmount(amt - remaining);
                remaining = 0;
            }
            if (remaining == 0) break;
        }
        return remaining == 0;
    }

    public static int count(org.bukkit.entity.Player player) {
        int total = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (isDanmuji(slot)) total += slot.getAmount();
        }
        return total;
    }
}
