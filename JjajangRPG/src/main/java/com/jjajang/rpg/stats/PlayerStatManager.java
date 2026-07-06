package com.jjajang.rpg.stats;

import com.jjajang.rpg.enhance.EnhanceManager;
import com.jjajang.rpg.refine.PotentialGrade;
import com.jjajang.rpg.refine.PotentialOption;
import com.jjajang.rpg.refine.RefineManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerStatManager {

    /** 플레이어의 전체 세공/강화 스탯 합산 */
    public static PlayerStats getStats(Player player) {
        PlayerStats stats = new PlayerStats();

        // 무기 (메인핸드)
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (EnhanceManager.isWeapon(weapon)) {
            stats.starWeaponAtk = EnhanceManager.getAttackBonus(EnhanceManager.getStars(weapon));
        }
        addPotentialStats(weapon, stats);

        // 갑옷 4종
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) continue;
            stats.starArmorDef += EnhanceManager.getDefenseBonus(EnhanceManager.getStars(piece));
            addPotentialStats(piece, stats);
        }

        return stats;
    }

    private static void addPotentialStats(ItemStack item, PlayerStats stats) {
        if (item == null || !item.hasItemMeta()) return;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        String gradeStr = pdc.get(RefineManager.KEY_GRADE, PersistentDataType.STRING);
        if (gradeStr == null) return;
        PotentialGrade grade;
        try { grade = PotentialGrade.valueOf(gradeStr); } catch (IllegalArgumentException e) { return; }

        for (org.bukkit.NamespacedKey lineKey : new org.bukkit.NamespacedKey[]{
                RefineManager.KEY_LINE0, RefineManager.KEY_LINE1, RefineManager.KEY_LINE2}) {
            String line = pdc.get(lineKey, PersistentDataType.STRING);
            if (line == null) continue;
            PotentialOption opt = PotentialOption.fromKey(line);
            if (opt == null || opt.disabled) continue;
            int val = PotentialOption.valueFromKey(line);
            switch (opt) {
                case ATTACK      -> stats.attackPct    += val;
                case DAMAGE      -> stats.damagePct    += val;
                case CRIT_DAMAGE -> stats.critDmgPct   += val;
                case CRIT_CHANCE -> stats.critChancePct+= val;
                case BOSS_DAMAGE -> stats.bossDmgPct   += val;
                case MOB_DAMAGE  -> stats.mobDmgPct    += val;
                case DEFENSE     -> stats.defenseFlat  += val;
                case GOLD_GAIN   -> stats.goldGainPct  += val;
                case DROP_RATE   -> stats.dropRatePct  += val;
                case MOVE_SPEED  -> stats.moveSpeedPct += val;
                // HP: 등급 기반 값 사용 (기존 아이템의 잘못된 PDC 값 무시)
                case HP          -> stats.hpFlat       += opt.getValue(grade);
                default -> {}
            }
        }
    }
}
