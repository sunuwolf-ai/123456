package com.jjajang.rpg.rogue;

import com.jjajang.rpg.util.SkillDamageTracker;
import com.jjajang.rpg.util.WeaponUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class RogueSkillListener implements Listener {

    private static final long CD_T1          = 10_000L;
    private static final long T1_COMBO_WINDOW = 5_000L;
    private static final long CD_T2          = 15_000L;
    private static final long CD_T3          = 60_000L;

    private final Map<UUID, Long> t1Used       = new HashMap<>();
    private final Map<UUID, Long> t1ComboUntil = new HashMap<>(); // 5초 콤보 창
    private final Map<UUID, Long> t2Used       = new HashMap<>();
    private final Map<UUID, Long> t3Used       = new HashMap<>();

    // 버프 만료 시각
    private final Map<UUID, Long> t1BleedUntil = new HashMap<>();
    private final Map<UUID, Long> t2DmgUntil   = new HashMap<>();

    private final JavaPlugin plugin;

    public RogueSkillListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!RogueItems.isRogueWeapon(held, plugin)) return;

        event.setCancelled(true);
        int tier = WeaponUtils.getWeaponTier(held, plugin);

        if (tier == 1)      handleT1(player, held);
        else if (tier == 2) handleT2(player, held);
        else if (tier == 3) handleT3(player);
    }

    // ── T1: 돌진 베기 (5초 내 재입력 시 2타) ────────────────────────
    private void handleT1(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        // ── 콤보 창 안에서 재입력 → 2타 ──
        if (t1ComboUntil.getOrDefault(id, 0L) > now) {
            t1ComboUntil.remove(id);
            t1Used.put(id, now); // 이제 쿨타임 시작
            performDash(player, held, id, true);
            return;
        }

        // 일반 쿨타임 체크
        long rem = CD_T1 - (now - t1Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }

        // ── 1타: 쿨타임 아직 시작 안 함, 콤보 창 열기 ──
        t1BleedUntil.put(id, now + 10_000L);
        t1ComboUntil.put(id, now + T1_COMBO_WINDOW);

        // 5초 후 콤보 미사용 → 쿨타임 시작
        new BukkitRunnable() {
            @Override public void run() {
                if (t1ComboUntil.containsKey(id)) {
                    t1ComboUntil.remove(id);
                    t1Used.put(id, now); // 1타 시각 기준 CD 시작
                }
            }
        }.runTaskLater(plugin, T1_COMBO_WINDOW / 50L);

        performDash(player, held, id, false);
    }

    private void performDash(Player player, ItemStack held, UUID id, boolean isSecond) {
        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 8.0 * pen;
        World world = player.getWorld();

        if (isSecond) {
            player.sendActionBar(Component.text("🗡 돌진 베기 2타!").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        } else {
            player.sendActionBar(Component.text("🗡 돌진 베기! [5초내 재입력 → 2타]")
                    .color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        }
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, isSecond ? 1.1f : 0.8f);

        // 전사 T2 방식: 속도 벡터로 즉발 돌진 — 벽은 물리 엔진이 차단하므로 대각선 관통 없음
        Vector dashDir = player.getLocation().getDirection().setY(0).normalize();
        player.setVelocity(dashDir.clone().multiply(isSecond ? 1.8 : 1.4).setY(0.18));

        Set<UUID> hit = new HashSet<>();

        // 돌진 중 피해 판정 (12틱 = 0.6초)
        new BukkitRunnable() {
            int tick = 0;

            @Override public void run() {
                if (tick >= 12 || !player.isOnline()) { this.cancel(); return; }

                Location cur = player.getLocation().clone().add(0, 0.9, 0);

                // 궤적 파티클
                world.spawnParticle(Particle.SWEEP_ATTACK, cur, 1, 0.2, 0.1, 0.2, 0);
                world.spawnParticle(Particle.DUST, cur, 3, 0.3, 0.4, 0.3,
                        new Particle.DustOptions(Color.fromRGB(160, 0, 230), 1.0f));
                world.spawnParticle(Particle.CRIT, cur.clone().add(0, 0.3, 0), 1, 0.2, 0.1, 0.2, 0.03);

                // 주변 적 피해
                for (Entity e : player.getNearbyEntities(1.5, 2.0, 1.5)) {
                    if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
                    if (hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());

                    SkillDamageTracker.mark(id);
                    le.damage(dmg, player);
                    SkillDamageTracker.clear(id);

                    Location eLoc = le.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.DUST, eLoc, 10, 0.3, 0.4, 0.3,
                            new Particle.DustOptions(Color.fromRGB(160, 0, 230), 1.3f));
                    world.spawnParticle(Particle.CRIT, eLoc, 5, 0.2, 0.2, 0.2, 0.05);
                    world.playSound(eLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.2f);
                    player.sendMessage(ChatColor.DARK_PURPLE + "[돌진 베기] " + ChatColor.WHITE
                            + le.getName() + " " + ChatColor.RED + String.format("%.1f", dmg) + " 피해"
                            + (pen < 1.0 ? ChatColor.GRAY + " (직업 패널티)" : ""));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ── T2: 가장 가까운 적 뒤로 순간이동 + 5초 딜증가 ───────────────
    private void handleT2(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T2 - (now - t2Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t2Used.put(id, now);

        World world = player.getWorld();

        LivingEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(20, 10, 20)) {
            if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
            double dist = e.getLocation().distanceSquared(player.getLocation());
            if (dist < minDist) { minDist = dist; nearest = le; }
        }

        if (nearest == null) {
            player.sendMessage(ChatColor.GRAY + "[기습 이동] 범위 내 적 없음");
            t2Used.put(id, 0L);
            return;
        }

        Vector targetDir = nearest.getLocation().getDirection().setY(0).normalize();
        Location behindLoc = nearest.getLocation().clone()
                .subtract(targetDir.multiply(2.0))
                .add(0, 0.1, 0);

        Vector toTarget = nearest.getLocation().toVector().subtract(behindLoc.toVector()).setY(0).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.getX(), toTarget.getZ()));
        behindLoc.setYaw(yaw);
        behindLoc.setPitch(0);
        player.teleport(behindLoc);

        t2DmgUntil.put(id, now + 5_000L);

        world.playSound(behindLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
        world.spawnParticle(Particle.DUST, behindLoc.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3,
                new Particle.DustOptions(Color.fromRGB(100, 0, 180), 1.2f));
        world.spawnParticle(Particle.CRIT, behindLoc.clone().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.05);

        player.sendActionBar(Component.text("🗡 기습 이동! 5초 딜증가 +30%").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        player.sendMessage(ChatColor.DARK_PURPLE + "[기습 이동] " + ChatColor.WHITE
                + nearest.getName() + " 뒤로 순간이동 + 5초 딜 +30%!");
    }

    // ── T3: 연막탄 (밀도 높은 연막 + 공격력 + 이속) ──────────────────
    private void handleT3(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T3 - (now - t3Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t3Used.put(id, now);

        World world = player.getWorld();
        Location center = player.getLocation().clone();
        final double SMOKE_RADIUS = 4.5;
        final double SMOKE_HEIGHT = 3.5;

        // 자신 버프: 은신 + 공격력 + 이속 (10초 = 200틱)
        applyInvisibility(player, 200);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,    200, 1, false, false, false));

        player.sendActionBar(Component.text("💨 연막탄! 은신+공격력↑+이속↑").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.5f);
        world.playSound(center, Sound.ENTITY_PHANTOM_FLAP,  0.8f, 0.4f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "[연막탄] " + ChatColor.WHITE
                + "연막 배치! 은신+공격력↑+이속↑ (10초), 연막 내 적 실명 10초!");

        // ─ 연막 루프 (200틱 = 10초) ──────────────────────────────────────
        new BukkitRunnable() {
            int tick = 0;

            @Override public void run() {
                if (tick >= 100) { this.cancel(); return; } // 2틱 × 100 = 200틱 = 10초

                // 볼륨 내부를 격자로 꽉 채워 연막 생성
                double gridStep = 1.3;
                for (double dx = -SMOKE_RADIUS; dx <= SMOKE_RADIUS; dx += gridStep) {
                    for (double dz = -SMOKE_RADIUS; dz <= SMOKE_RADIUS; dz += gridStep) {
                        if (dx*dx + dz*dz > SMOKE_RADIUS * SMOKE_RADIUS) continue;
                        for (double dy = 0.3; dy <= SMOKE_HEIGHT; dy += 1.2) {
                            double jx = (Math.random() - 0.5) * gridStep;
                            double jz = (Math.random() - 0.5) * gridStep;
                            Location p = center.clone().add(dx + jx, dy, dz + jz);
                            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 1,
                                    0, 0.01, 0, 0.003);
                        }
                    }
                }

                // 가장자리 테두리 연기 (연막 윤곽)
                if (tick % 3 == 0) {
                    for (int a = 0; a < 360; a += 15) {
                        double rad = Math.toRadians(a);
                        Location edge = center.clone().add(
                                Math.cos(rad) * SMOKE_RADIUS, 1.0 + Math.random() * SMOKE_HEIGHT,
                                Math.sin(rad) * SMOKE_RADIUS);
                        world.spawnParticle(Particle.CLOUD, edge, 1, 0.1, 0.05, 0.1, 0.02);
                    }
                }

                // 5틱마다 내부 적 실명 — 200틱(10초) 지속으로 꽉 채움
                if (tick % 5 == 0) {
                    for (Entity e : player.getNearbyEntities(SMOKE_RADIUS + 2, SMOKE_HEIGHT, SMOKE_RADIUS + 2)) {
                        if (!(e instanceof LivingEntity nearby) || e.equals(player)) continue;
                        if (nearby.getLocation().distanceSquared(center) <= (SMOKE_RADIUS + 2) * (SMOKE_RADIUS + 2)) {
                            nearby.addPotionEffect(new PotionEffect(
                                    PotionEffectType.DARKNESS, 200, 0, false, false, false));
                        }
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ── 투명화 + 장비 숨김 (아이템 삭제 없이 가짜 패킷으로 처리) ───────
    private void applyInvisibility(Player player, int durationTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                durationTicks, 0, false, false, false));

        Map<EquipmentSlot, ItemStack> empty = buildEmptyEquip();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.hidePlayer(plugin, player);
            // 가짜 빈 장비 패킷 전송 → 다른 플레이어 화면에서 갑옷/무기 안 보임
            online.sendEquipmentChange(player, empty);
        }

        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) return;
                Map<EquipmentSlot, ItemStack> real = buildRealEquip(player);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.showPlayer(plugin, player);
                    // 실제 장비 복원 패킷 전송
                    online.sendEquipmentChange(player, real);
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }

    private static Map<EquipmentSlot, ItemStack> buildEmptyEquip() {
        Map<EquipmentSlot, ItemStack> m = new EnumMap<>(EquipmentSlot.class);
        ItemStack air = new ItemStack(Material.AIR);
        m.put(EquipmentSlot.HAND,      air);
        m.put(EquipmentSlot.OFF_HAND,  air);
        m.put(EquipmentSlot.HEAD,      air);
        m.put(EquipmentSlot.CHEST,     air);
        m.put(EquipmentSlot.LEGS,      air);
        m.put(EquipmentSlot.FEET,      air);
        return m;
    }

    private static Map<EquipmentSlot, ItemStack> buildRealEquip(Player player) {
        PlayerInventory inv = player.getInventory();
        Map<EquipmentSlot, ItemStack> m = new EnumMap<>(EquipmentSlot.class);
        m.put(EquipmentSlot.HAND,      orAir(inv.getItemInMainHand()));
        m.put(EquipmentSlot.OFF_HAND,  orAir(inv.getItemInOffHand()));
        m.put(EquipmentSlot.HEAD,      orAir(inv.getHelmet()));
        m.put(EquipmentSlot.CHEST,     orAir(inv.getChestplate()));
        m.put(EquipmentSlot.LEGS,      orAir(inv.getLeggings()));
        m.put(EquipmentSlot.FEET,      orAir(inv.getBoots()));
        return m;
    }

    private static ItemStack orAir(ItemStack item) {
        return (item != null) ? item : new ItemStack(Material.AIR);
    }

    // ── 출혈 적용 ──────────────────────────────────────────────────────
    private void applyBleedOnHit(Player player, LivingEntity target, double pen) {
        World world = target.getWorld();
        int particleTicks = (int) (30 * pen);
        int bleedTicks    = Math.max(1, (int) Math.round(3 * pen));
        double bleedDmg   = 0.8 * pen; // 밸런스 패치: 출혈 데미지 감소 (1.5→0.8)

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick >= particleTicks || !target.isValid()) { this.cancel(); return; }
                Location tLoc = target.getLocation();
                for (int i = 0; i < 2; i++) {
                    world.spawnParticle(Particle.DUST,
                        tLoc.clone().add((Math.random()-0.5)*0.8, Math.random()*2.0, (Math.random()-0.5)*0.8),
                        1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.9f));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick >= bleedTicks || !target.isValid()) { this.cancel(); return; }
                target.damage(bleedDmg);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.8, 0),
                    2, 0.15, 0.1, 0.15, 0);
                tick++;
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    // ── 패시브: 기습 1.5배 + T1출혈 + T2딜증가 ────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRogueHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!RogueItems.isRogueWeapon(held, plugin)) return;
        if (SkillDamageTracker.isSkill(player.getUniqueId())) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 기습 패시브: 등 뒤 공격 시 ×1.5 (전 티어)
        Vector attackerToTarget = target.getLocation().subtract(player.getLocation()).toVector().setY(0);
        Vector targetFacing     = target.getLocation().getDirection().setY(0);
        if (attackerToTarget.length() > 0 && targetFacing.length() > 0) {
            attackerToTarget.normalize();
            targetFacing.normalize();
            if (attackerToTarget.dot(targetFacing) > 0.5) {
                event.setDamage(event.getDamage() * 1.5);
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3,
                        new Particle.DustOptions(Color.fromRGB(150, 0, 200), 1.2f));
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
                player.sendActionBar(Component.text("🗡 기습! ×1.5").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
            }
        }

        // T2 딜증가 버프 (+30%)
        if (t2DmgUntil.getOrDefault(id, 0L) > now) {
            event.setDamage(event.getDamage() * 1.3);
        }

        // T1 출혈 버프 (타 직업 무기 사용 시 지속시간도 감소)
        if (t1BleedUntil.getOrDefault(id, 0L) > now) {
            double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
            applyBleedOnHit(player, target, pen);
        }
    }

    private void cooldownBar(Player player, long remainMs) {
        player.sendActionBar(Component.text("⏳ 쿨다운 " + (int) Math.ceil(remainMs / 1000.0) + "초")
                .color(NamedTextColor.RED));
    }
}
