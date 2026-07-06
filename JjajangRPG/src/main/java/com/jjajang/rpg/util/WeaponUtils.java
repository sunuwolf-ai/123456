package com.jjajang.rpg.util;

import com.jjajang.rpg.classes.ClassManager;
import com.jjajang.rpg.classes.PlayerClass;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class WeaponUtils {

    public static final String KEY_CLASS = "weapon_class";
    public static final String KEY_TIER  = "weapon_tier";

    public static void tagWeapon(ItemMeta meta, JavaPlugin plugin, String weaponClass, int tier) {
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_CLASS), PersistentDataType.STRING, weaponClass);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_TIER),  PersistentDataType.INTEGER, tier);
    }

    public static String getWeaponClass(ItemStack item, JavaPlugin plugin) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, KEY_CLASS), PersistentDataType.STRING);
    }

    public static int getWeaponTier(ItemStack item, JavaPlugin plugin) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, KEY_TIER), PersistentDataType.INTEGER, 0);
    }

    /** 플레이어 직업과 무기 직업이 일치하는지 확인 */
    public static boolean isOwnClassWeapon(Player player, ItemStack item, JavaPlugin plugin) {
        String wClass = getWeaponClass(item, plugin);
        if (wClass == null) return true; // 일반 무기는 패널티 없음
        PlayerClass cls = ClassManager.getClass(player);
        return wClass.equals(cls.getWeaponClass());
    }

    /** 타 직업 무기 사용 시 0.5, 본인 직업이면 1.0 */
    public static double getPenaltyMultiplier(Player player, ItemStack item, JavaPlugin plugin) {
        return isOwnClassWeapon(player, item, plugin) ? 1.0 : 0.5;
    }

    /** 인벤토리(메인+핫바)에서 특정 직업 무기를 전부 제거 */
    public static void removeWeaponOfClass(Player player, String weaponClass, JavaPlugin plugin) {
        if (weaponClass == null) return;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (weaponClass.equals(getWeaponClass(contents[i], plugin))) {
                player.getInventory().setItem(i, null);
            }
        }
    }
}
