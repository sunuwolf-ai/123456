package com.jjajang.rpg.refine;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

public class RefineManager {

    public static NamespacedKey KEY_GRADE;
    public static NamespacedKey KEY_LINE0, KEY_LINE1, KEY_LINE2;
    public static NamespacedKey KEY_ROLLS; // ceiling counter
    private static final int CEILING = 50;
    private static final Random RNG = new Random();

    public static void init(JavaPlugin plugin) {
        KEY_GRADE = new NamespacedKey(plugin, "potential_grade");
        KEY_LINE0 = new NamespacedKey(plugin, "potential_line0");
        KEY_LINE1 = new NamespacedKey(plugin, "potential_line1");
        KEY_LINE2 = new NamespacedKey(plugin, "potential_line2");
        KEY_ROLLS = new NamespacedKey(plugin, "potential_rolls");
    }

    /** 세공 가능한 아이템인지 */
    public static boolean isRefineable(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_HOE")
            || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT")
            || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
            || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    public static PotentialGrade getGrade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String g = item.getItemMeta().getPersistentDataContainer()
                       .get(KEY_GRADE, PersistentDataType.STRING);
        if (g == null) return null;
        try { return PotentialGrade.valueOf(g); } catch (IllegalArgumentException e) { return null; }
    }

    public static int getRolls(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                   .getOrDefault(KEY_ROLLS, PersistentDataType.INTEGER, 0);
    }

    public static String[] getLines(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new String[3];
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return new String[]{
            pdc.get(KEY_LINE0, PersistentDataType.STRING),
            pdc.get(KEY_LINE1, PersistentDataType.STRING),
            pdc.get(KEY_LINE2, PersistentDataType.STRING)
        };
    }

    /**
     * 세공 1회 실행.
     * @return "CEILING_UP" if grade upgraded, "ROLLED" otherwise
     */
    public static String roll(JavaPlugin plugin, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 현재 등급 가져오기 (없으면 EPIC 시작)
        String gradeStr = pdc.getOrDefault(KEY_GRADE, PersistentDataType.STRING, "EPIC");
        PotentialGrade grade = PotentialGrade.valueOf(gradeStr);

        int rolls = pdc.getOrDefault(KEY_ROLLS, PersistentDataType.INTEGER, 0) + 1;
        boolean upgraded = false;

        // 천장: 50회 → 등급 업
        if (rolls >= CEILING && !grade.isMax()) {
            grade = grade.next();
            rolls = 0;
            upgraded = true;
        } else if (rolls >= CEILING && grade.isMax()) {
            rolls = 0; // 레전드리 50회: 리셋만
        }

        // 3줄 랜덤 생성 (해당 등급에서 사용 가능한 옵션만)
        final PotentialGrade finalGrade = grade;
        PotentialOption[] options = java.util.Arrays.stream(PotentialOption.values())
                .filter(o -> o.isAvailableFor(finalGrade))
                .toArray(PotentialOption[]::new);
        String line0 = options[RNG.nextInt(options.length)].serialize(grade);
        String line1 = options[RNG.nextInt(options.length)].serialize(grade);
        String line2 = options[RNG.nextInt(options.length)].serialize(grade);

        pdc.set(KEY_GRADE, PersistentDataType.STRING, grade.name());
        pdc.set(KEY_LINE0, PersistentDataType.STRING, line0);
        pdc.set(KEY_LINE1, PersistentDataType.STRING, line1);
        pdc.set(KEY_LINE2, PersistentDataType.STRING, line2);
        pdc.set(KEY_ROLLS, PersistentDataType.INTEGER, rolls);

        item.setItemMeta(meta);
        com.jjajang.rpg.enhance.EnhanceManager.refreshLore(plugin, item);
        return upgraded ? "CEILING_UP" : "ROLLED";
    }

    /** EnhanceManager.refreshLore() 에서 호출 - 세공 라인을 lore에 추가 */
    public static void appendPotentialLore(JavaPlugin plugin, PersistentDataContainer pdc, List<String> lore) {
        String gradeStr = pdc.get(KEY_GRADE, PersistentDataType.STRING);
        if (gradeStr == null) return;
        PotentialGrade grade;
        try { grade = PotentialGrade.valueOf(gradeStr); } catch (IllegalArgumentException e) { return; }

        String l0 = pdc.get(KEY_LINE0, PersistentDataType.STRING);
        String l1 = pdc.get(KEY_LINE1, PersistentDataType.STRING);
        String l2 = pdc.get(KEY_LINE2, PersistentDataType.STRING);
        if (l0 == null) return;

        int rolls = pdc.getOrDefault(KEY_ROLLS, PersistentDataType.INTEGER, 0);

        lore.add(ChatColor.DARK_GRAY + "──────────────────");
        lore.add(ChatColor.DARK_PURPLE + "잠재능력 " + grade.color + "[" + grade.displayName + "]");
        for (String line : new String[]{l0, l1, l2}) {
            if (line == null) continue;
            PotentialOption opt = PotentialOption.fromKey(line);
            if (opt == null) continue;
            lore.add(ChatColor.GRAY + "  ▸ " + ChatColor.YELLOW + opt.displayName + " "
                    + ChatColor.GREEN + opt.formatValue(grade));
        }
        lore.add(ChatColor.DARK_GRAY + "세공 횟수: " + ChatColor.GRAY + rolls + "/" + CEILING);
    }
}
