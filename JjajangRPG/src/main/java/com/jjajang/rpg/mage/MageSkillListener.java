package com.jjajang.rpg.mage;

import com.jjajang.rpg.classes.ClassManager;
import com.jjajang.rpg.util.SkillDamageTracker;
import com.jjajang.rpg.util.WeaponUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MageSkillListener implements Listener {

    private static final long CD_T1    = 6_000L;
    private static final long CD_T2    = 9_000L;
    private static final long CD_T3    = 16_000L;
    private static final long BASIC_CD = 450L;
    private static final long KILL_CD_REDUCE = 1_500L; // 패시브: 처치 시 쿨타임 감소

    private final Map<UUID, Long> t1Used    = new HashMap<>();
    private final Map<UUID, Long> t2Used    = new HashMap<>();
    private final Map<UUID, Long> t3Used    = new HashMap<>();
    private final Map<UUID, Long> basicUsed = new HashMap<>();

    private final JavaPlugin plugin;

    public MageSkillListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── 우클릭: 티어 스킬 ────────────────────────────────────────────
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!MageItems.isMageWeapon(held, plugin)) return;

        event.setCancelled(true);
        int tier = WeaponUtils.getWeaponTier(held, plugin);
        if (tier == 1)      handleT1(player, held);
        else if (tier == 2) handleT2(player, held);
        else if (tier == 3) handleT3(player, held);
    }

    // ── 좌클릭(허공/블록): 기본공격 에너지볼 ───────────────────────────
    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!MageItems.isMageWeapon(held, plugin)) return;

        event.setCancelled(true);
        handleBasicAttack(player, held);
    }

    // ── 좌클릭(적 직접 타격): 바닐라 데미지 무효화 + 기본공격으로 대체 ───
    @EventHandler(priority = EventPriority.LOW)
    public void onMeleeAttempt(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (SkillDamageTracker.isSkill(player.getUniqueId())) return; // 재진입 방지

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!MageItems.isMageWeapon(held, plugin)) return;
        event.setCancelled(true);

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - basicUsed.getOrDefault(id, 0L) < BASIC_CD) return;
        basicUsed.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 3.0 * pen;
        World world = player.getWorld();

        SkillDamageTracker.mark(id);
        target.damage(dmg, player);
        SkillDamageTracker.clear(id);

        Location loc = target.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.CRIT, loc, 6, 0.2, 0.2, 0.2, 0.05);
        world.spawnParticle(Particle.DUST, loc, 8, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.fromRGB(120, 180, 255), 1.0f));
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 1.3f);
    }

    // ── 기본공격: 약한 에너지볼 (단일 대상) ──────────────────────────
    private void handleBasicAttack(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - basicUsed.getOrDefault(id, 0L) < BASIC_CD) return;
        basicUsed.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 3.0 * pen;
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 1.6f);

        launchOrb(player, 1.1, 16.0, 0.9,
                loc -> world.spawnParticle(Particle.WITCH, loc, 2, 0.05, 0.05, 0.05, 0),
                (target, loc) -> {
                    SkillDamageTracker.mark(id);
                    target.damage(dmg, player);
                    SkillDamageTracker.clear(id);
                    world.spawnParticle(Particle.CRIT, loc, 6, 0.2, 0.2, 0.2, 0.05);
                    world.spawnParticle(Particle.DUST, loc, 8, 0.2, 0.2, 0.2,
                            new Particle.DustOptions(Color.fromRGB(120, 180, 255), 1.0f));
                    world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 1.3f);
                });
    }

    // ── T1: 파이어볼 (단일, 화염 피해) ────────────────────────────────
    private void handleT1(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T1 - (now - t1Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t1Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 11.0 * pen;
        World world = player.getWorld();
        player.sendActionBar(Component.text("🔥 파이어볼!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f);

        launchOrb(player, 0.9, 22.0, 1.0,
                loc -> world.spawnParticle(Particle.FLAME, loc, 4, 0.08, 0.08, 0.08, 0.01),
                (target, loc) -> {
                    SkillDamageTracker.mark(id);
                    target.damage(dmg, player);
                    SkillDamageTracker.clear(id);
                    target.setFireTicks((int) (60 * pen)); // 타 직업 무기 사용 시 효과도 감소

                    world.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.LAVA, loc, 5, 0.2, 0.2, 0.2, 0);
                    world.playSound(loc, Sound.ENTITY_GENERIC_BURN, 0.8f, 1.0f);
                    player.sendMessage(ChatColor.GOLD + "[파이어볼] " + ChatColor.WHITE + target.getName()
                            + " " + ChatColor.RED + String.format("%.1f", dmg) + " 화염 피해");
                });
    }

    // ── T2: 연쇄 에너지볼 (주변 적에게 최대 5회 추가 전이) ─────────────
    private void handleT2(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T2 - (now - t2Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t2Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 7.0 * pen;
        World world = player.getWorld();
        player.sendActionBar(Component.text("⚡ 연쇄 에너지볼!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.4f);

        Set<UUID> hitSet = new HashSet<>();
        launchOrb(player, 1.2, 24.0, 1.1,
                loc -> world.spawnParticle(Particle.DUST, loc, 3, 0.08, 0.08, 0.08,
                        new Particle.DustOptions(Color.fromRGB(60, 200, 255), 1.1f)),
                (target, loc) -> chainHit(player, id, target, loc, dmg, hitSet, 0));
    }

    private void chainHit(Player player, UUID id, LivingEntity target, Location loc,
                           double dmg, Set<UUID> hitSet, int jump) {
        if (!target.isValid()) return;
        hitSet.add(target.getUniqueId());
        World world = player.getWorld();

        SkillDamageTracker.mark(id);
        target.damage(dmg, player);
        SkillDamageTracker.clear(id);

        world.spawnParticle(Particle.DUST, loc, 12, 0.3, 0.4, 0.3,
                new Particle.DustOptions(Color.fromRGB(60, 200, 255), 1.3f));
        world.spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2, 0.05);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.5f);
        player.sendMessage(ChatColor.AQUA + "[연쇄 에너지볼] " + ChatColor.WHITE + target.getName()
                + " " + ChatColor.RED + String.format("%.1f", dmg) + " 피해"
                + (jump > 0 ? ChatColor.GRAY + " (전이 " + jump + "회)" : ""));

        if (jump >= 5) return; // 최대 5회 추가 전이

        LivingEntity next = null;
        double bestDist = 6.0;
        for (Entity e : target.getNearbyEntities(6.0, 4.0, 6.0)) {
            if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
            if (hitSet.contains(le.getUniqueId())) continue;
            double d = le.getLocation().distance(target.getLocation());
            if (d < bestDist) { bestDist = d; next = le; }
        }
        if (next == null) return;

        LivingEntity finalNext = next;
        new BukkitRunnable() {
            @Override public void run() {
                chainHit(player, id, finalNext, finalNext.getLocation().add(0, 1, 0), dmg, hitSet, jump + 1);
            }
        }.runTaskLater(plugin, 4L);
    }

    // ── T3: 멸절의 광선 (보는 방향으로 긴 즉시 라인 피해) ──────────────
    private void handleT3(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T3 - (now - t3Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t3Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 24.0 * pen; // 밸런스 패치: 3차 스킬 데미지 증가 (16→24)
        World world = player.getWorld();
        player.sendActionBar(Component.text("☄ 멸절의 광선!").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.2f);

        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        double range = 28.0;
        double step = 0.5;

        Set<UUID> hit = new HashSet<>();
        Location point = start.clone();
        for (double d = 0; d < range; d += step) {
            point.add(dir.clone().multiply(step));
            world.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05,
                    new Particle.DustOptions(Color.fromRGB(0, 220, 255), 1.4f));
            world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);

            if (!point.getBlock().isPassable()) break;

            for (Entity e : world.getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
                if (hit.contains(le.getUniqueId())) continue;
                hit.add(le.getUniqueId());

                SkillDamageTracker.mark(id);
                le.damage(dmg, player);
                SkillDamageTracker.clear(id);

                Location eLoc = le.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.EXPLOSION, eLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.DUST, eLoc, 15, 0.3, 0.4, 0.3,
                        new Particle.DustOptions(Color.fromRGB(0, 220, 255), 1.5f));
                world.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
                player.sendMessage(ChatColor.DARK_AQUA + "[멸절의 광선] " + ChatColor.WHITE + le.getName()
                        + " " + ChatColor.RED + String.format("%.1f", dmg) + " 피해");
            }
        }
    }

    // ── 패시브: 적 처치 시 쿨타임 감소 ──────────────────────────────
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!ClassManager.getClass(killer).isMage()) return;

        UUID id = killer.getUniqueId();
        boolean reduced = false;
        if (t1Used.containsKey(id)) { t1Used.put(id, t1Used.get(id) - KILL_CD_REDUCE); reduced = true; }
        if (t2Used.containsKey(id)) { t2Used.put(id, t2Used.get(id) - KILL_CD_REDUCE); reduced = true; }
        if (t3Used.containsKey(id)) { t3Used.put(id, t3Used.get(id) - KILL_CD_REDUCE); reduced = true; }

        if (reduced) {
            killer.sendActionBar(Component.text("💀 처치! 쿨타임 1.5초 감소").color(NamedTextColor.LIGHT_PURPLE));
        }
    }

    // ── 공용: 전방으로 날아가는 시각적 투사체 (실제 엔티티 아님) ────────
    private void launchOrb(Player player, double speed, double maxDistance, double hitRadius,
                            Consumer<Location> particleTrail,
                            BiConsumer<LivingEntity, Location> onHit) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        World world = player.getWorld();

        new BukkitRunnable() {
            double traveled = 0;
            final Location current = start.clone();

            @Override public void run() {
                if (traveled >= maxDistance || !player.isOnline()) { this.cancel(); return; }
                current.add(dir.clone().multiply(speed));
                traveled += speed;
                particleTrail.accept(current);

                if (!current.getBlock().isPassable()) { this.cancel(); return; }

                for (Entity e : world.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                    if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
                    onHit.accept(le, current.clone());
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void cooldownBar(Player player, long remainMs) {
        player.sendActionBar(Component.text("⏳ 쿨다운 " + (int) Math.ceil(remainMs / 1000.0) + "초")
                .color(NamedTextColor.RED));
    }
}
