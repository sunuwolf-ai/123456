package com.jjajang.rpg.stats;

import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

public class StatListener implements Listener {

    private final NamespacedKey KEY_HP;
    private final NamespacedKey KEY_SPEED;
    private final GoldManager gm;
    private final GoldScoreboard sb;
    private final JavaPlugin plugin;

    public StatListener(JavaPlugin plugin, GoldManager gm, GoldScoreboard sb) {
        this.plugin = plugin;
        this.gm = gm;
        this.sb = sb;
        this.KEY_HP    = new NamespacedKey(plugin, "stat_hp");
        this.KEY_SPEED = new NamespacedKey(plugin, "stat_speed");
    }

    // ── 데미지 적용 (HIGHEST: WeaponPenaltyListener HIGH 이후) ─────────
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player attacker) {
            PlayerStats stats = PlayerStatManager.getStats(attacker);
            double dmg = event.getDamage();

            dmg += stats.starWeaponAtk;

            if (stats.attackPct > 0) dmg *= (1.0 + stats.attackPct / 100.0);
            if (stats.damagePct > 0) dmg *= (1.0 + stats.damagePct / 100.0);

            Entity target = event.getEntity();
            if (isBoss(target) && stats.bossDmgPct > 0) {
                dmg *= (1.0 + stats.bossDmgPct / 100.0);
            } else if (target instanceof Monster && stats.mobDmgPct > 0) {
                dmg *= (1.0 + stats.mobDmgPct / 100.0);
            }

            if (stats.critChancePct > 0 && Math.random() * 100 < stats.critChancePct) {
                double critMult = 1.0 + stats.critDmgPct / 100.0;
                dmg *= critMult;
                attacker.sendActionBar(Component.text("💥 크리티컬!").color(NamedTextColor.YELLOW));
                target.getWorld().spawnParticle(Particle.CRIT,
                        target.getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.3f);
            }

            event.setDamage(dmg);
        }

        if (event.getEntity() instanceof Player victim) {
            PlayerStats stats = PlayerStatManager.getStats(victim);

            if (stats.evasionPct > 0 && Math.random() * 100 < stats.evasionPct) {
                event.setCancelled(true);
                victim.sendActionBar(Component.text("🌀 회피!").color(NamedTextColor.AQUA));
                victim.getWorld().spawnParticle(Particle.WITCH, victim.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.05);
                return;
            }

            int totalDef = stats.starArmorDef + stats.defenseFlat;
            if (totalDef > 0) {
                double reductionPct = Math.min(totalDef * 0.1, 75.0);
                event.setDamage(event.getDamage() * (1.0 - reductionPct / 100.0));
            }
        }
    }

    // ── 몬스터/플레이어 처치 시 골드 ─────────────────────────────────
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Player victim) {
            long victimGold = gm.get(victim.getUniqueId());
            if (victimGold > 0) {
                long stolen = (long)(victimGold * 0.3);
                if (stolen < 1) stolen = 1;
                gm.spend(victim.getUniqueId(), stolen);
                gm.add(killer.getUniqueId(), stolen);
                sb.update(killer);
                sb.update(victim);
                killer.sendActionBar(Component.text("+" + stolen + "G (PvP 강탈!)").color(NamedTextColor.GOLD));
                victim.sendMessage(ChatColor.RED + "[PvP] " + killer.getName() + "에게 " + stolen + "G를 뺏겼습니다!");
            }
            return;
        }

        long baseGold = getBaseGold(event.getEntityType());
        if (baseGold <= 0) return;

        PlayerStats stats = PlayerStatManager.getStats(killer);
        double multiplier = 1.0 + stats.goldGainPct / 100.0;
        long earned = (long)(baseGold * multiplier);

        gm.add(killer.getUniqueId(), earned);
        sb.update(killer);
        killer.sendActionBar(Component.text("+" + earned + "G").color(NamedTextColor.GOLD));

        if (stats.dropRatePct > 0 && Math.random() * 100 < stats.dropRatePct
                && !event.getDrops().isEmpty()) {
            event.getDrops().add(event.getDrops().get(0).clone());
        }
    }

    // ── 속성 갱신 트리거 ─────────────────────────────────────────────
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
            applyAttributes(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
            applyAttributes(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
            applyAttributes(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // 사망 즉시 HP 보너스 제거
        removeAllHpModifiers(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
            applyAttributes(event.getPlayer()), 2L);
    }

    // ── HP 수정자 전체 제거 (기존 버그 수정자 포함) ───────────────────
    public void removeAllHpModifiers(Player player) {
        AttributeInstance maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp == null) return;
        // 전사 패시브는 setBaseValue() 방식 → 수정자(modifier)에 해당 없음 → 전부 제거해도 안전
        new ArrayList<>(maxHp.getModifiers()).forEach(maxHp::removeModifier);
        // 현재 체력이 최대체력 초과 시 클램프
        double maxVal = maxHp.getValue();
        if (player.getHealth() > maxVal) player.setHealth(maxVal);
    }

    // ── 속성 계산 및 적용 ─────────────────────────────────────────────
    public void applyAttributes(Player player) {
        PlayerStats stats = PlayerStatManager.getStats(player);

        // HP: 기존 수정자 전부 제거 후 신규 NamespacedKey 방식으로 재적용
        AttributeInstance maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) {
            new ArrayList<>(maxHp.getModifiers()).forEach(maxHp::removeModifier);
            if (stats.hpFlat > 0) {
                maxHp.addModifier(new AttributeModifier(
                    KEY_HP, stats.hpFlat,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY));
            }
            double maxVal = maxHp.getValue();
            if (player.getHealth() > maxVal) player.setHealth(maxVal);
        }

        // 이동속도
        AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(KEY_SPEED);
            if (stats.moveSpeedPct > 0) {
                speed.addModifier(new AttributeModifier(
                    KEY_SPEED, stats.moveSpeedPct / 100.0,
                    AttributeModifier.Operation.ADD_SCALAR,
                    EquipmentSlotGroup.ANY));
            }
        }

        // 점프력
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        if (stats.jumpLevel > 0) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, stats.jumpLevel - 1, true, false, false));
        }
    }

    private static boolean isBoss(Entity e) {
        return e instanceof WitherSkeleton || e instanceof Wither
            || e instanceof EnderDragon || e instanceof ElderGuardian;
    }

    private static long getBaseGold(org.bukkit.entity.EntityType type) {
        return switch (type) {
            case WITHER, ENDER_DRAGON, ELDER_GUARDIAN, WARDEN -> 500L;
            default -> 200L;
        };
    }
}
