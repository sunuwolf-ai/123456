package com.jjajang.rpg.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class WeaponPenaltyListener implements Listener {

    private final JavaPlugin plugin;

    public WeaponPenaltyListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 타 직업 무기 데미지 50% 감소 (스킬은 각 스킬 코드에서 이미 처리) */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamagePenalty(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (SkillDamageTracker.isSkill(player.getUniqueId())) return; // 스킬은 직접 처리

        ItemStack held = player.getInventory().getItemInMainHand();
        String wClass = WeaponUtils.getWeaponClass(held, plugin);
        if (wClass == null) return;

        if (!WeaponUtils.isOwnClassWeapon(player, held, plugin)) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    /** 일반 공격 데미지 채팅 표시 (스킬 피해는 제외) */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDisplay(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (SkillDamageTracker.isSkill(player.getUniqueId())) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (WeaponUtils.getWeaponClass(held, plugin) == null) return;

        boolean penalty = !WeaponUtils.isOwnClassWeapon(player, held, plugin);
        String penaltyTag = penalty ? ChatColor.GRAY + " (직업 패널티)" : "";
        player.sendMessage(ChatColor.DARK_GRAY + "  ▸ " + target.getName() + "에게 "
                + ChatColor.RED + String.format("%.1f", event.getFinalDamage())
                + " 데미지" + penaltyTag);
    }
}
