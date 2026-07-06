package com.jjajang.rpg.enhance;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EnhanceManager {

    // PDC Keys
    public static NamespacedKey KEY_STARS;
    public static NamespacedKey KEY_DESTROYED;
    public static NamespacedKey KEY_SOURCE_MAT; // material name for restoration
    public static NamespacedKey KEY_BASE_LORE;  // original lore before enhancement

    public static void init(JavaPlugin plugin) {
        KEY_STARS       = new NamespacedKey(plugin, "enhance_stars");
        KEY_DESTROYED   = new NamespacedKey(plugin, "enhance_destroyed");
        KEY_SOURCE_MAT  = new NamespacedKey(plugin, "enhance_source_mat");
        KEY_BASE_LORE   = new NamespacedKey(plugin, "enhance_base_lore");
    }

    // ── Starforce rates ────────────────────────────────────────────────────
    // [successPct, failPct, destroyPct]  (must sum to 100)
    private static final double[][] RATES = {
        {95, 5, 0},   // 0→1
        {90, 10, 0},  // 1→2
        {85, 15, 0},  // 2→3
        {85, 15, 0},  // 3→4
        {80, 20, 0},  // 4→5
        {75, 25, 0},  // 5→6
        {70, 30, 0},  // 6→7
        {65, 35, 0},  // 7→8
        {60, 40, 0},  // 8→9
        {55, 45, 0},  // 9→10
        {50, 50, 0},  // 10→11  (fail=drop)
        {45, 55, 0},  // 11→12
        {40, 60, 0},  // 12→13
        {35, 65, 0},  // 13→14
        {30, 70, 0},  // 14→15
        {30, 67, 3},  // 15→16  (destroy 시작)
        {27, 68, 5},  // 16→17
        {24, 69, 7},  // 17→18
        {21, 70, 9},  // 18→19
        {18, 72, 10}, // 19→20
    };

    /** 강화 결과: SUCCESS / FAIL / DESTROY */
    public enum Result { SUCCESS, FAIL, DESTROY }

    public static Result roll(int currentStar) {
        if (currentStar >= 20) return Result.FAIL;
        double[] r = RATES[currentStar];
        double rand = Math.random() * 100;
        if (rand < r[0]) return Result.SUCCESS;
        if (rand < r[0] + r[1]) return Result.FAIL;
        return Result.DESTROY;
    }

    /** 강화 비용 */
    public static long getCost(int currentStar) {
        if (currentStar < 5)  return 100;
        if (currentStar < 10) return 300;
        if (currentStar < 15) return 1000;
        return 3000;
    }

    /** 성 → 무기 공격력 보너스 (누적) */
    public static int getAttackBonus(int stars) {
        if (stars <= 0) return 0;
        int bonus = 0;
        for (int i = 1; i <= stars; i++) {
            if (i <= 5)       bonus += 2;
            else if (i <= 10) bonus += 3;
            else if (i <= 15) bonus += 5;
            else              bonus += 8;
        }
        return bonus;
    }

    /** 성 → 방어구 방어력 보너스 (누적) */
    public static int getDefenseBonus(int stars) {
        if (stars <= 0) return 0;
        int bonus = 0;
        for (int i = 1; i <= stars; i++) {
            if (i <= 5)       bonus += 1;
            else if (i <= 10) bonus += 2;
            else if (i <= 15) bonus += 3;
            else              bonus += 5;
        }
        return bonus;
    }

    // ── PDC helpers ────────────────────────────────────────────────────────
    public static int getStars(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(KEY_STARS, PersistentDataType.INTEGER, 0);
    }

    public static boolean isDestroyed(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(KEY_DESTROYED, PersistentDataType.BYTE);
    }

    public static boolean isEnhanceable(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return isWeapon(item) || isArmor(item);
    }

    public static boolean isWeapon(ItemStack item) {
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_HOE")
            || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT");
    }

    public static boolean isArmor(ItemStack item) {
        String n = item.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
            || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    /** 별 수 설정 및 lore 갱신 */
    public static void setStars(JavaPlugin plugin, ItemStack item, int stars) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_STARS, PersistentDataType.INTEGER, Math.max(0, Math.min(20, stars)));
        item.setItemMeta(meta);
        refreshLore(plugin, item);
    }

    /** 파괴 처리 - item을 "흔적" 아이템으로 변환 */
    public static void destroy(JavaPlugin plugin, ItemStack item) {
        String origName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();
        ItemMeta meta = item.getItemMeta();
        // 원본 재료 저장
        meta.getPersistentDataContainer().set(KEY_SOURCE_MAT, PersistentDataType.STRING, item.getType().name());
        meta.getPersistentDataContainer().set(KEY_DESTROYED, PersistentDataType.BYTE, (byte) 1);
        // 별 초기화
        meta.getPersistentDataContainer().set(KEY_STARS, PersistentDataType.INTEGER, 0);
        meta.setDisplayName(ChatColor.DARK_GRAY + "☠ [흔적] " + ChatColor.RESET + origName);
        meta.setLore(List.of(
            ChatColor.RED + "파괴된 장비입니다.",
            ChatColor.GRAY + "같은 종류의 장비를 재료로",
            ChatColor.GRAY + "넣어 10성에서 복원할 수 있습니다."
        ));
        item.setItemMeta(meta);
    }

    /** lore 갱신 (별 + 세공 통합) */
    public static void refreshLore(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        int stars = pdc.getOrDefault(KEY_STARS, PersistentDataType.INTEGER, 0);

        // base lore 저장 (최초 1회)
        List<String> existingLore = meta.hasLore() ? meta.getLore() : List.of();
        if (!pdc.has(KEY_BASE_LORE, PersistentDataType.STRING)) {
            // 이미 RPG lore 가 없을 때만 저장
            String joined = String.join("\n", existingLore);
            pdc.set(KEY_BASE_LORE, PersistentDataType.STRING, joined);
        }
        String baseRaw = pdc.getOrDefault(KEY_BASE_LORE, PersistentDataType.STRING, "");
        List<String> lore = new ArrayList<>();
        if (!baseRaw.isEmpty()) {
            for (String l : baseRaw.split("\n")) lore.add(l);
        }

        // 강화 라인
        if (stars > 0) {
            lore.add(ChatColor.GOLD + "★ " + stars + "성 " + ChatColor.GRAY + "(" + stars + "/20)");
            boolean weapon = isWeapon(item);
            int bonus = weapon ? getAttackBonus(stars) : getDefenseBonus(stars);
            String statName = weapon ? "공격력" : "방어력";
            lore.add(ChatColor.GRAY + statName + " +" + ChatColor.GREEN + bonus);
        }

        // 세공 라인 (RefineManager에서 채움)
        com.jjajang.rpg.refine.RefineManager.appendPotentialLore(plugin, pdc, lore);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static double[] getRates(int star) {
        if (star < 0 || star >= RATES.length) return new double[]{0, 100, 0};
        return RATES[star];
    }
}
