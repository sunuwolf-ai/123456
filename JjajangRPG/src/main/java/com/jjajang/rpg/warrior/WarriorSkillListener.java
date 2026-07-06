package com.jjajang.rpg.warrior;

import com.jjajang.rpg.classes.ClassManager;
import com.jjajang.rpg.util.SkillDamageTracker;
import com.jjajang.rpg.util.WeaponUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarriorSkillListener implements Listener {

    private static final long CD_T1 = 22_000L;
    private static final long CD_T2 = 20_000L;
    private static final long CD_T3 = 90_000L;
    private static final long T2_WINDOW = 5_000L;

    private final Map<UUID, Long> t1Used = new HashMap<>();
    private final Map<UUID, Long> t2Used = new HashMap<>();
    private final Map<UUID, Long> t3Used = new HashMap<>();
    private final Map<UUID, Boolean> t2Ready = new HashMap<>();

    private final JavaPlugin plugin;

    public WarriorSkillListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WarriorItems.isWarriorWeapon(held, plugin)) return;

        event.setCancelled(true);
        int tier = WeaponUtils.getWeaponTier(held, plugin);

        if (tier == 1)      handleT1(player, held);
        else if (tier == 2) handleT2(player, held);
        else if (tier == 3) handleT3(player, held);
    }

    // ── T1: 전방 베기 ──────────────────────────────────────────────────
    private void handleT1(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T1 - (now - t1Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t1Used.put(id, now);

        double raw = 10.0;
        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = raw * pen;

        player.sendActionBar(Component.text("⚔ 전방 베기!")
                .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.1f);

        Location loc = player.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        World world = player.getWorld();

        // 붉은 베기 이펙트
        spawnSlashArc(world, loc, dir, 2.0, 5);

        for (Entity e : player.getNearbyEntities(2.0, 2.0, 2.0)) {
            if (!(e instanceof LivingEntity t) || e.equals(player)) continue;
            Vector toT = t.getLocation().subtract(loc).toVector().setY(0).normalize();
            if (toT.dot(dir) < 0.75) continue;

            SkillDamageTracker.mark(id);
            t.damage(dmg, player);
            SkillDamageTracker.clear(id);

            spawnHitBurst(world, t.getLocation().add(0, 1, 0));
            world.playSound(t.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            sendDmgMsg(player, "⚔ 전방 베기", t.getName(), dmg, pen < 1.0);
        }
    }

    // ── T2: 검찌르기 → 회전베기 ────────────────────────────────────────
    private void handleT2(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (t2Ready.getOrDefault(id, false)) {
            if (now - t2Used.getOrDefault(id, 0L) <= T2_WINDOW) {
                t2Ready.put(id, false);
                doSpinSlash(player, held);
                return;
            }
            t2Ready.put(id, false);
        }

        long rem = CD_T2 - (now - t2Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }

        t2Used.put(id, now);
        t2Ready.put(id, true);
        doThrust(player, held);
        new BukkitRunnable() { @Override public void run() { t2Ready.put(id, false); } }
                .runTaskLater(plugin, T2_WINDOW / 50);
    }

    private void doThrust(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        double dmg = 8.0 * WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);

        player.sendActionBar(Component.text("⚔ 검찌르기!  ").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                .append(Component.text("[ 5초 내 우클릭 → 회전베기 ]").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.2f);

        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        player.setVelocity(dir.clone().multiply(1.8).setY(0.25));

        new BukkitRunnable() {
            @Override public void run() {
                Location loc = player.getLocation();
                World world = player.getWorld();

                // 붉은+검정 돌진 파티클
                for (double d = 0.4; d <= 2.8; d += 0.35) {
                    Location p = loc.clone().add(dir.clone().multiply(d)).add(0, 1, 0);
                    world.spawnParticle(Particle.DUST, p, 2, 0.05, 0.2, 0.05,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.3f));
                    world.spawnParticle(Particle.CRIT, p, 1, 0.05, 0.1, 0.05, 0);
                }

                for (Entity e : player.getNearbyEntities(3, 2, 3)) {
                    if (!(e instanceof LivingEntity t) || e.equals(player)) continue;
                    Vector toT = t.getLocation().subtract(loc).toVector().setY(0).normalize();
                    if (toT.dot(dir) < 0.5) continue;

                    SkillDamageTracker.mark(id);
                    t.damage(dmg, player);
                    SkillDamageTracker.clear(id);

                    spawnHitBurst(world, t.getLocation().add(0, 1, 0));
                    world.playSound(t.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                    sendDmgMsg(player, "⚔ 검찌르기", t.getName(), dmg, pen < 1.0);
                }
            }
        }.runTaskLater(plugin, 7L);
    }

    private void doSpinSlash(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        double baseDmg = 7.0 * WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);

        player.sendActionBar(Component.text("🌀 회전베기! (5초)").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);

        // 피해 쿨다운: 같은 적을 너무 자주 안 때리게 (0.5초 = 10틱)
        java.util.Map<UUID, Integer> hitCooldown = new java.util.HashMap<>();

        new BukkitRunnable() {
            int tick = 0;

            @Override public void run() {
                if (tick >= 100) { this.cancel(); return; } // 100틱 = 5초

                double radius = 4.0 + tick * 0.03;
                World world = player.getWorld();
                Location center = player.getLocation();

                // 외곽 링 파티클
                for (int angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle + tick * 36);
                    Location p = center.clone().add(Math.cos(rad) * radius, 1.0, Math.sin(rad) * radius);
                    world.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.DUST, p, 2, 0.1, 0.1, 0.1,
                            new Particle.DustOptions(tick % 2 == 0 ? Color.fromRGB(200, 0, 0) : Color.fromRGB(10, 0, 0), 1.2f));
                }

                // 내부 채움 파티클 (3틱마다) — 안에도 빨간 기운이 보이게
                if (tick % 3 == 0) {
                    for (double r = 1.0; r < radius - 0.5; r += 1.5) {
                        for (int angle = 0; angle < 360; angle += 45) {
                            double rad = Math.toRadians(angle + tick * 18);
                            Location p = center.clone().add(Math.cos(rad) * r, 0.8 + Math.random() * 0.6, Math.sin(rad) * r);
                            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0,
                                    new Particle.DustOptions(Color.fromRGB(160, 0, 0), 0.9f));
                        }
                    }
                }

                // 피해 판정: 10틱(0.5초) 쿨다운으로 내·외부 모두 적중
                hitCooldown.replaceAll((k, v) -> v - 1);
                hitCooldown.values().removeIf(v -> v <= 0);

                for (Entity e : player.getNearbyEntities(radius + 1, 3, radius + 1)) {
                    if (!(e instanceof LivingEntity t) || e.equals(player)) continue;
                    // 원형 판정 (박스 아닌 거리 체크)
                    double dist = e.getLocation().distanceSquared(center);
                    if (dist > (radius + 0.5) * (radius + 0.5)) continue;
                    if (hitCooldown.containsKey(e.getUniqueId())) continue;

                    SkillDamageTracker.mark(id);
                    t.damage(baseDmg, player);
                    SkillDamageTracker.clear(id);

                    spawnHitBurst(world, t.getLocation().add(0, 1, 0));
                    sendDmgMsg(player, "🌀 회전베기", t.getName(), baseDmg, pen < 1.0);
                    hitCooldown.put(e.getUniqueId(), 10); // 0.5초 쿨다운
                }

                if (tick % 20 == 0) {
                    int remaining = (100 - tick) / 20;
                    player.sendActionBar(Component.text("🌀 회전베기 " + remaining + "초...")
                            .color(NamedTextColor.RED));
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── T3: 불굴의 의지 ────────────────────────────────────────────────
    private void handleT3(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T3 - (now - t3Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t3Used.put(id, now);

        player.sendActionBar(Component.text("💥 불굴의 의지!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);

        // 회복량 (타 직업 무기 사용 시 회복량도 감소)
        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double healAmount = (maxHp - player.getHealth()) * pen;
        player.setHealth(Math.min(maxHp, player.getHealth() + healAmount));

        // 회복 이펙트
        World world = player.getWorld();
        Location center = player.getLocation();
        world.spawnParticle(Particle.HEART, center.clone().add(0, 2, 0), 8, 0.5, 0.3, 0.5, 0);
        world.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 30, 1.0, 1.5, 1.0,
                new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));

        player.sendMessage(ChatColor.RED + "[불굴의 의지] " + ChatColor.WHITE + "체력이 완전히 회복되었습니다!");

        // 주변 2블록 내 적 넉백
        double kbRadius = 2.0;
        int knocked = 0;
        for (Entity e : player.getNearbyEntities(kbRadius, kbRadius, kbRadius)) {
            if (!(e instanceof LivingEntity t) || e.equals(player)) continue;

            // 플레이어→적 방향으로 날려보내기
            Vector kb = e.getLocation().subtract(player.getLocation()).toVector().setY(0);
            if (kb.lengthSquared() < 0.001) kb = new Vector(1, 0, 0);
            kb.normalize().multiply(2.5).setY(0.6);
            e.setVelocity(kb);

            world.spawnParticle(Particle.DUST, e.getLocation().add(0, 1, 0), 10, 0.4, 0.5, 0.4,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
            knocked++;
        }

        // 넉백 폭발 이펙트
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            Location p = center.clone().add(Math.cos(rad) * kbRadius, 0.5, Math.sin(rad) * kbRadius);
            world.spawnParticle(Particle.DUST, p, 3, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.8f));
            world.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0, 0, 0, 0);
        }
        world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);

        if (knocked > 0)
            player.sendMessage(ChatColor.DARK_RED + "[불굴의 의지] " + ChatColor.WHITE + knocked + "명 넉백!");
    }

    // ── 이펙트 헬퍼 ───────────────────────────────────────────────────
    private void spawnSlashArc(World world, Location origin, Vector dir, double length, int points) {
        for (int i = 0; i < points; i++) {
            double d = (i + 1) * (length / points);
            Location p = origin.clone().add(dir.clone().multiply(d)).add(0, 1.1, 0);
            world.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST, p, 3, 0.15, 0.3, 0.15,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.4f));
            world.spawnParticle(Particle.DUST, p, 1, 0.05, 0.1, 0.05,
                    new Particle.DustOptions(Color.fromRGB(10, 0, 0), 1.0f));
        }
    }

    private void spawnHitBurst(World world, Location loc) {
        world.spawnParticle(Particle.CRIT, loc, 6, 0.2, 0.2, 0.2, 0.1);
        world.spawnParticle(Particle.DUST, loc, 5, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.fromRGB(160, 0, 0), 1.5f));
        world.spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1,
                new Particle.DustOptions(Color.fromRGB(5, 0, 0), 1.0f));
        world.spawnParticle(Particle.DAMAGE_INDICATOR, loc, 3, 0.2, 0.2, 0.2, 0);
    }

    private void cooldownBar(Player player, long remainMs) {
        player.sendActionBar(Component.text("⏳ 쿨다운 " + (int) Math.ceil(remainMs / 1000.0) + "초")
                .color(NamedTextColor.RED));
    }

    private void sendDmgMsg(Player player, String skill, String target, double dmg, boolean penalty) {
        player.sendMessage(ChatColor.YELLOW + "[" + skill + "] " + ChatColor.WHITE
                + target + "에게 " + ChatColor.RED + String.format("%.1f", dmg) + " 데미지"
                + (penalty ? ChatColor.GRAY + " (직업 패널티)" : ""));
    }
}
