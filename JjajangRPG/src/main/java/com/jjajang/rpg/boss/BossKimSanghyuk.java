package com.jjajang.rpg.boss;

import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import com.jjajang.rpg.item.BoarHornItem;
import com.jjajang.rpg.item.BoarLeatherItem;
import com.jjajang.rpg.item.BoarMeatItem;
import com.jjajang.rpg.item.BossFragmentItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BossKimSanghyuk implements Listener {

    private static final String DISPLAY_NAME = "멧돼지 김상혁";
    private static final double MAX_HP = 350.0;
    private static final double HEAL_THRESHOLD = 50.0;
    private static final double HEAL_AMOUNT = 150.0;

    private static NamespacedKey BOSS_KEY;

    private final JavaPlugin plugin;
    private final GoldManager gm;
    private final GoldScoreboard sb;

    // 상태 추적
    private final Map<UUID, BossBar> bossBars     = new HashMap<>();
    private final Set<UUID> groggyBosses          = new HashSet<>(); // 보스 그로기
    private final Set<UUID> healedBosses          = new HashSet<>(); // 이미 회복함
    private final Set<UUID> chargingBosses        = new HashSet<>(); // 돌진 중

    public BossKimSanghyuk(JavaPlugin plugin, GoldManager gm, GoldScoreboard sb) {
        this.plugin = plugin;
        this.gm = gm;
        this.sb = sb;
        BOSS_KEY = new NamespacedKey(plugin, "boss_kimsh");
    }

    // ── 소환 ──────────────────────────────────────────────────────
    public void spawn(Location loc) {
        Hoglin boss = (Hoglin) loc.getWorld().spawnEntity(loc, EntityType.HOGLIN);
        boss.setImmuneToZombification(true);
        boss.customName(Component.text(DISPLAY_NAME).color(NamedTextColor.RED));
        boss.setCustomNameVisible(true);

        AttributeInstance maxHp = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(MAX_HP);
        boss.setHealth(MAX_HP);

        AttributeInstance speed = boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.28);

        // 크기 2배 (히트박스 포함)
        AttributeInstance scale = boss.getAttribute(Attribute.GENERIC_SCALE);
        if (scale != null) scale.setBaseValue(2.0);

        boss.getPersistentDataContainer().set(BOSS_KEY, PersistentDataType.BYTE, (byte) 1);

        BossBar bar = Bukkit.createBossBar(
                ChatColor.RED + "✦ " + DISPLAY_NAME + " ✦",
                BarColor.RED, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        bossBars.put(boss.getUniqueId(), bar);

        loc.getWorld().playSound(loc, Sound.ENTITY_HOGLIN_ANGRY, 1.5f, 0.7f);
        loc.getWorld().strikeLightningEffect(loc);
        Bukkit.broadcast(Component.text(
                "☠ 멧돼지 김상혁이 출현했다! (" +
                (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + ")")
                .color(NamedTextColor.RED));

        startLoop(boss, bar);
    }

    // ── 메인 루프 ─────────────────────────────────────────────────
    private void startLoop(Hoglin boss, BossBar bar) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) {
                    bar.removeAll();
                    bossBars.remove(boss.getUniqueId());
                    cancel();
                    return;
                }

                // 보스바 업데이트 (20틱마다)
                if (tick % 20 == 0) {
                    double maxHp = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    bar.setProgress(Math.max(0.0, boss.getHealth() / maxHp));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        double dist = p.getWorld().equals(boss.getWorld())
                                ? p.getLocation().distance(boss.getLocation()) : 9999;
                        if (dist < 80) bar.addPlayer(p);
                        else bar.removePlayer(p);
                    }
                }

                // 밥먹기: 체력 50 미만, 1회성
                if (!healedBosses.contains(boss.getUniqueId()) && boss.getHealth() < HEAL_THRESHOLD) {
                    healedBosses.add(boss.getUniqueId());
                    doEat(boss);
                }

                // 스킬 쿨다운 (그로기·돌진 중엔 스킬 없음)
                UUID id = boss.getUniqueId();
                boolean groggy   = groggyBosses.contains(id);
                boolean charging = chargingBosses.contains(id);

                if (!groggy && !charging) {
                    // 물기: 4초마다
                    if (tick % 80 == 20) doBite(boss);
                    // 돌진: 10초마다
                    if (tick % 200 == 100) doCharge(boss);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    // ── 스킬: 물기 ────────────────────────────────────────────────
    private void doBite(Hoglin boss) {
        Player target = nearestPlayer(boss, 8);
        if (target == null) return;
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 1f, 1f);
        target.damage(12.0, boss);
        target.sendActionBar(Component.text("김상혁이 물었다! -12 HP").color(NamedTextColor.RED));
    }

    // ── 스킬: 돌진 ────────────────────────────────────────────────
    private void doCharge(Hoglin boss) {
        // 보스 주위에 플레이어가 없으면 돌진 스킵
        if (nearestPlayer(boss, 30) == null) return;

        chargingBosses.add(boss.getUniqueId());
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1f, 0.5f);

        // 현재 보스가 보는 방향으로만 돌진 (조준 회피 가능)
        Vector dir = boss.getLocation().getDirection().setY(0).normalize();

        // 돌진 경고 파티클
        Location from = boss.getLocation().clone();
        boss.getWorld().spawnParticle(Particle.DUST,
                from.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.RED, 1.5f));

        new BukkitRunnable() {
            int tick = 0;
            Location prevLoc = boss.getLocation().clone();

            @Override
            public void run() {
                tick++;
                if (!boss.isValid() || boss.isDead()) {
                    chargingBosses.remove(boss.getUniqueId()); cancel(); return;
                }
                if (tick > 50) { // 최대 2.5초
                    chargingBosses.remove(boss.getUniqueId()); cancel(); return;
                }

                boss.setVelocity(dir.clone().multiply(0.85).setY(boss.getVelocity().getY()));

                // 돌진 파티클
                boss.getWorld().spawnParticle(Particle.DUST,
                        boss.getLocation().add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 80, 80), 1.0f));

                // 벽 충돌 감지: 이동거리가 너무 작으면
                double moved = boss.getLocation().distance(prevLoc);
                prevLoc = boss.getLocation().clone();
                if (tick > 5 && moved < 0.08) {
                    chargingBosses.remove(boss.getUniqueId());
                    enterBossGroggy(boss);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 3L, 1L);
    }

    // ── 스킬: 밥먹기 ──────────────────────────────────────────────
    private void doEat(Hoglin boss) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.5f, 0.8f);
        boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0, 2, 0), 8, 0.5, 0.5, 0.5, 0);

        Bukkit.broadcast(Component.text(
                "☠ 멧돼지 김상혁이 밥을 먹기 시작했다...").color(NamedTextColor.YELLOW));

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) { cancel(); return; }
                count++;
                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 0.9f);
                boss.getWorld().spawnParticle(Particle.HEART,
                        boss.getLocation().add(0, 2, 0), 3, 0.4, 0.4, 0.4, 0);

                double newHp = Math.min(boss.getHealth() + HEAL_AMOUNT / 20.0,
                        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                boss.setHealth(newHp);

                if (count >= 20) { // 1초에 걸쳐 회복
                    Bukkit.broadcast(Component.text(
                            "☠ 멧돼지 김상혁이 " + (int)HEAL_AMOUNT + " HP를 회복했다!")
                            .color(NamedTextColor.RED));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── 보스 그로기 ───────────────────────────────────────────────
    private void enterBossGroggy(Hoglin boss) {
        UUID id = boss.getUniqueId();
        groggyBosses.add(id);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4, false, false));
        boss.getWorld().spawnParticle(Particle.WITCH, boss.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_HOGLIN_HURT, 1.5f, 0.4f);

        Bukkit.broadcast(Component.text(
                "★ 멧돼지 김상혁이 벽에 박았다! 그로기 상태! (5초)")
                .color(NamedTextColor.GOLD));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            groggyBosses.remove(id);
            if (boss.isValid())
                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1f, 1f);
        }, 100L);
    }

    // ── 이벤트: 데미지 수신 ───────────────────────────────────────
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        if (!isBoss(le)) return;
        // 그로기 중엔 2배 데미지
        if (groggyBosses.contains(le.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);
            le.getWorld().spawnParticle(Particle.CRIT,
                    le.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.1);
        }
    }

    // ── 이벤트: 처치 ──────────────────────────────────────────────
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isBoss(event.getEntity())) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        Location loc = event.getEntity().getLocation();
        loc.getWorld().dropItemNaturally(loc, BoarHornItem.create(2));
        loc.getWorld().dropItemNaturally(loc, BoarLeatherItem.create(3));
        loc.getWorld().dropItemNaturally(loc, BoarMeatItem.create(5));
        loc.getWorld().dropItemNaturally(loc, BossFragmentItem.create(1));

        loc.getWorld().createExplosion(loc, 0f, false, false);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 5, 1, 1, 1, 0);

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            gm.add(killer.getUniqueId(), 2009523L);
            sb.update(killer);
            killer.sendMessage(ChatColor.GOLD + "[보스] 멧돼지 김상혁 처치! +2,009,523G");
            killer.sendActionBar(Component.text("+2,009,523G").color(NamedTextColor.GOLD));
        }

        UUID id = event.getEntity().getUniqueId();
        BossBar bar = bossBars.remove(id);
        if (bar != null) bar.removeAll();
        groggyBosses.remove(id);
        healedBosses.remove(id);
        chargingBosses.remove(id);

        Bukkit.broadcast(Component.text(
                "☠ 멧돼지 김상혁이 쓰러졌다!" +
                (killer != null ? " (" + killer.getName() + " 처치)" : ""))
                .color(NamedTextColor.GOLD));
    }

    // ── 유틸 ──────────────────────────────────────────────────────
    public boolean isBoss(Entity entity) {
        if (!(entity instanceof LivingEntity le)) return false;
        return le.getPersistentDataContainer().has(BOSS_KEY, PersistentDataType.BYTE);
    }

    private Player nearestPlayer(Entity entity, double maxRange) {
        Player nearest = null;
        double minDist = maxRange * maxRange;
        for (Entity e : entity.getNearbyEntities(maxRange, maxRange, maxRange)) {
            if (!(e instanceof Player p)) continue;
            double dist = entity.getLocation().distanceSquared(p.getLocation());
            if (dist < minDist) { minDist = dist; nearest = p; }
        }
        return nearest;
    }
}
