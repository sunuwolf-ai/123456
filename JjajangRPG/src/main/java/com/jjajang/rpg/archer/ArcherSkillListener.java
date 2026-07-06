package com.jjajang.rpg.archer;

import com.jjajang.rpg.util.WeaponUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.NamespacedKey;

import java.util.*;

public class ArcherSkillListener implements Listener {

    private static final long CD_T1  = 24_000L; // 밸런스 패치: 쿨타임 증가 (18→24초)
    private static final long CD_T2  = 12_000L; // 밸런스 패치: 쿨타임 증가 (8→12초)
    private static final long CD_T3  = 90_000L;
    private static final double BASIC_RANGE = 18.0; // 밸런스 패치: 기본공격 사거리 감소

    private final Map<UUID, Long>    t1Used       = new HashMap<>();
    private final Map<UUID, Long>    t2Used       = new HashMap<>();
    private final Map<UUID, Long>    t3Used       = new HashMap<>();
    private final Map<UUID, Integer> t2ArrowsLeft = new HashMap<>();
    // 화살 엔티티 UUID → 타입 (onShootBow 태깅 백업)
    private final Map<UUID, String>  t2ArrowEntities = new HashMap<>();
    // 표식 패시브: 대상 UUID → 표식을 남긴 궁수 UUID (중첩 불가, 단일 표식만 유지)
    private final Map<UUID, UUID>    marks = new HashMap<>();

    private final JavaPlugin   plugin;
    private final NamespacedKey arrowTypeKey;
    private final NamespacedKey bombArrowKey;
    private final NamespacedKey t2ArrowKey;

    public ArcherSkillListener(JavaPlugin plugin) {
        this.plugin       = plugin;
        this.arrowTypeKey = new NamespacedKey(plugin, "arrow_type");
        this.bombArrowKey = new NamespacedKey(plugin, "bomb_arrow");
        this.t2ArrowKey   = new NamespacedKey(plugin, "t2_arrow");
    }

    @EventHandler
    public void onFKey(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ArcherItems.isArcherWeapon(held, plugin)) return;

        event.setCancelled(true);
        int tier = WeaponUtils.getWeaponTier(held, plugin);

        if (tier == 1)      handleT1(player, held);
        else if (tier == 2) handleT2(player, held);
        else if (tier == 3) handleT3(player, held);
    }

    // ── T1: 도약 속사 ──────────────────────────────────────────────────
    private void handleT1(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T1 - (now - t1Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t1Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        player.sendActionBar(Component.text("🏹 도약 속사!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);

        Vector backDir = player.getLocation().getDirection().setY(0).normalize().multiply(-1.5).setY(0.55);
        player.setVelocity(backDir);

        Location eyeLoc = player.getEyeLocation();
        Vector aimDir   = eyeLoc.getDirection();
        World world     = player.getWorld();

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override public void run() {
                    Vector spread = aimDir.clone().add(new Vector(
                            (Math.random() - 0.5) * 0.05,
                            (Math.random() - 0.5) * 0.05,
                            (Math.random() - 0.5) * 0.05));
                    Arrow arrow = world.spawnArrow(player.getEyeLocation(), spread, 1.8f, 1.5f);
                    arrow.setShooter(player);
                    arrow.setDamage(5.0 * pen);
                    arrow.getPersistentDataContainer().set(arrowTypeKey, PersistentDataType.STRING, "t1_quick");
                    world.spawnParticle(Particle.DUST, arrow.getLocation(), 3, 0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.fromRGB(100, 255, 50), 1.0f));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.3f + idx * 0.05f);
                }
            }.runTaskLater(plugin, (long)(i * 3));
        }
    }

    // ── T2: 특수화살 3종 한꺼번에 지급 ───────────────────────────────
    private void handleT2(Player player, ItemStack held) {
        UUID id = player.getUniqueId();

        int existing = countT2Arrows(player);
        if (existing > 0) {
            player.sendActionBar(Component.text("🏹 특수화살 " + existing + "발 보유 중!")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        long now = System.currentTimeMillis();
        long rem = CD_T2 - (now - t2Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }

        giveAllT2Arrows(player);
    }

    private void giveAllT2Arrows(Player player) {
        player.getInventory().addItem(makePoison(5));
        player.getInventory().addItem(makeIce(5));
        player.getInventory().addItem(makeBombArrow(1));
        t2ArrowsLeft.put(player.getUniqueId(), 11);

        player.sendActionBar(Component.text("🏹 속성화살 지급! ☠독×5 ❄얼음×5 💣폭탄×1")
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.4f);
    }

    private ItemStack makePoison(int amount) {
        ItemStack item = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta m = (PotionMeta) item.getItemMeta();
        m.setBasePotionType(PotionType.POISON);
        m.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 160, 0, false, true, true), true);
        m.setDisplayName(ChatColor.DARK_GREEN + "☠ 독 화살");
        m.setLore(List.of(ChatColor.GRAY + "독 데미지 8초 지속", ChatColor.DARK_GRAY + "궁수 2차 속성화살"));
        m.getPersistentDataContainer().set(t2ArrowKey,   PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(arrowTypeKey, PersistentDataType.STRING, "poison");
        item.setItemMeta(m);
        return item;
    }

    private ItemStack makeIce(int amount) {
        ItemStack item = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta m = (PotionMeta) item.getItemMeta();
        m.setBasePotionType(PotionType.SLOWNESS);
        m.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 9, false, true, true), true);
        m.setDisplayName(ChatColor.AQUA + "❄ 얼음 화살");
        m.setLore(List.of(ChatColor.GRAY + "완전 이동 봉쇄 5초 (느린 투사체)", ChatColor.DARK_GRAY + "궁수 2차 속성화살"));
        m.getPersistentDataContainer().set(t2ArrowKey,   PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(arrowTypeKey, PersistentDataType.STRING, "ice");
        item.setItemMeta(m);
        return item;
    }

    private ItemStack makeBombArrow(int amount) {
        ItemStack item = new ItemStack(Material.TIPPED_ARROW, amount);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(ChatColor.RED + "💣 폭탄 화살");
        m.setLore(List.of(ChatColor.GRAY + "명중 시 폭발 발생", ChatColor.DARK_GRAY + "궁수 2차 속성화살"));
        m.getPersistentDataContainer().set(t2ArrowKey,   PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(bombArrowKey, PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(arrowTypeKey, PersistentDataType.STRING, "bomb");
        item.setItemMeta(m);
        return item;
    }

    // ── 활 발사 감지 ─────────────────────────────────────────────────
    // getConsumable()이 실패할 수 있으므로 인벤토리 탐색 fallback 포함
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        // ── Layer 1: consumable PDC ──
        String type = null;
        boolean isBomb = false;

        ItemStack consumable = event.getConsumable();
        if (consumable != null && consumable.hasItemMeta()) {
            var pdc = consumable.getItemMeta().getPersistentDataContainer();
            if (pdc.has(t2ArrowKey, PersistentDataType.BYTE)) {
                type   = pdc.get(arrowTypeKey, PersistentDataType.STRING);
                isBomb = pdc.has(bombArrowKey,  PersistentDataType.BYTE);
            }
        }

        // ── Layer 2: consumable이 null일 때만 인벤토리 탐색 fallback ──
        // consumable이 non-null이면 getConsumable()이 정상 작동한 것이므로
        // 일반화살이 소모된 경우 Layer 2를 건너뜀
        if (type == null && consumable == null) {
            for (ItemStack s : player.getInventory().getContents()) {
                if (s == null || !s.hasItemMeta()) continue;
                var pdc = s.getItemMeta().getPersistentDataContainer();
                if (!pdc.has(t2ArrowKey, PersistentDataType.BYTE)) continue;
                String t = pdc.get(arrowTypeKey, PersistentDataType.STRING);
                if (t != null) {
                    type   = t;
                    isBomb = pdc.has(bombArrowKey, PersistentDataType.BYTE);
                    break;
                }
            }
        }

        if (type == null) {
            // 기본공격(일반 화살): 밸런스 패치로 사거리 제한
            Location origin = player.getEyeLocation();
            new BukkitRunnable() {
                @Override public void run() {
                    if (!arrow.isValid()) { this.cancel(); return; }
                    if (arrow.getLocation().distance(origin) >= BASIC_RANGE) {
                        arrow.getWorld().spawnParticle(Particle.SMOKE, arrow.getLocation(), 4, 0.1, 0.1, 0.1, 0.02);
                        arrow.remove();
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 2L, 2L);
            return; // T2 화살 아님
        }

        final String finalType = type;

        // 화살 엔티티 PDC 태깅 (onArrowHit 1차 감지용)
        arrow.getPersistentDataContainer().set(arrowTypeKey, PersistentDataType.STRING, finalType);
        // UUID 맵 백업 (onArrowHit 2차 감지용)
        t2ArrowEntities.put(arrow.getUniqueId(), finalType);

        // 소진 추적 + 자동 재충전
        UUID id = player.getUniqueId();
        new BukkitRunnable() {
            @Override public void run() {
                int remaining = countT2Arrows(player);
                if (remaining == 0 && t2ArrowsLeft.containsKey(id)) {
                    t2ArrowsLeft.remove(id);
                    t2Used.put(id, System.currentTimeMillis());
                    player.sendActionBar(Component.text("화살 소진! " + (CD_T2 / 1000) + "초 후 재충전")
                            .color(NamedTextColor.YELLOW));
                    new BukkitRunnable() {
                        @Override public void run() {
                            if (!player.isOnline()) return;
                            giveAllT2Arrows(player);
                            player.sendActionBar(Component.text("🏹 특수화살 재충전!")
                                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                        }
                    }.runTaskLater(plugin, CD_T2 / 50L);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private int countT2Arrows(Player player) {
        int total = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.hasItemMeta()
                    && s.getItemMeta().getPersistentDataContainer().has(t2ArrowKey, PersistentDataType.BYTE)) {
                total += s.getAmount();
            }
        }
        return total;
    }

    // ── T3: 화살 비 ──────────────────────────────────────────────────
    private void handleT3(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T3 - (now - t3Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t3Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        player.sendActionBar(Component.text("🏹 화살 비!").color(NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.2f);

        Location center = player.getLocation().clone();
        World world     = player.getWorld();
        double radius   = 12.0;
        int totalArrows = 70;

        player.sendMessage(ChatColor.GREEN + "[화살 비] " + ChatColor.WHITE + totalArrows + "발 쏟아진다!");

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(radius, 10, radius)) {
            if (e instanceof LivingEntity le && !e.equals(player)) targets.add(le);
        }

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick >= 20) { this.cancel(); return; }
                for (int a = 0; a < 8; a++) {
                    double angle = Math.toRadians(a * 45 + tick * 18);
                    double dist  = Math.random() * radius;
                    Location p = center.clone().add(Math.cos(angle) * dist, 18 - tick * 0.3, Math.sin(angle) * dist);
                    world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(100, 255, 50), 1.2f));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (int i = 0; i < totalArrows; i++) {
            new BukkitRunnable() {
                @Override public void run() {
                    Location targetLoc;
                    if (!targets.isEmpty()) {
                        LivingEntity t = targets.get((int)(Math.random() * targets.size()));
                        targetLoc = t.isValid()
                            ? t.getLocation().clone().add((Math.random()-0.5)*1.5, 0, (Math.random()-0.5)*1.5)
                            : randomInRadius(center, radius);
                    } else {
                        targetLoc = randomInRadius(center, radius);
                    }
                    Location spawnLoc = targetLoc.clone().add(0, 20, 0);
                    Vector dir = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
                    Arrow arrow = world.spawnArrow(spawnLoc, dir, 2.5f, 2.0f);
                    arrow.setShooter(player);
                    arrow.setDamage(13.0 * pen); // 밸런스 패치: 3차 스킬 데미지 증가 (9→13)
                    arrow.getPersistentDataContainer().set(arrowTypeKey, PersistentDataType.STRING, "t3_rain");
                }
            }.runTaskLater(plugin, (long)(Math.random() * 60));
        }
    }

    private Location randomInRadius(Location center, double radius) {
        double ang = Math.random() * 360;
        double d   = Math.random() * radius * 0.7;
        return center.clone().add(Math.cos(Math.toRadians(ang)) * d, 0, Math.sin(Math.toRadians(ang)) * d);
    }

    // ── 패시브: 표식 시스템 (모든 화살 적중에 적용, 중첩 불가) ─────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMarkSystem(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target.equals(shooter)) return;

        ItemStack held = shooter.getInventory().getItemInMainHand();
        if (!ArcherItems.isArcherWeapon(held, plugin)) return;

        UUID tId = target.getUniqueId();
        UUID sId = shooter.getUniqueId();

        if (sId.equals(marks.get(tId))) {
            // 표식 소모 → 추가 피해 1.5배
            marks.remove(tId);
            event.setDamage(event.getDamage() * 1.5);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3,
                    new Particle.DustOptions(Color.fromRGB(255, 80, 0), 1.3f));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.6f);
            shooter.sendActionBar(Component.text("🎯 표식 적중! ×1.5").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        } else {
            marks.put(tId, sId);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0), 6, 0.2, 0.2, 0.2,
                    new Particle.DustOptions(Color.fromRGB(255, 220, 0), 1.0f));
            shooter.sendActionBar(Component.text("🎯 표식 부착!").color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler
    public void onMarkedDeath(EntityDeathEvent event) {
        marks.remove(event.getEntity().getUniqueId());
    }

    // ── 자기 스킬 화살 피해 면역 ─────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSelfArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!event.getEntity().equals(shooter)) return;
        if (arrow.getPersistentDataContainer().has(arrowTypeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // ── 속성 화살 적중 처리 ───────────────────────────────────────────
    // 3중 감지 레이어:
    //   1차: 엔티티 PDC (onShootBow에서 태깅)
    //   2차: t2ArrowEntities UUID 맵
    //   3차: TippedArrow 포션 효과 직접 분석
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        // 1차: 엔티티 PDC
        String type = arrow.getPersistentDataContainer().get(arrowTypeKey, PersistentDataType.STRING);

        // 2차: UUID 맵 (PDC 태깅이 실패한 경우)
        if (type == null) {
            type = t2ArrowEntities.remove(arrow.getUniqueId());
        }

        // 3차: TippedArrow 효과 분석 (이 플레이어가 T2 화살을 사용 중인 경우만)
        if (type == null && arrow instanceof TippedArrow tipped
                && (t2ArrowsLeft.containsKey(player.getUniqueId()) || countT2Arrows(player) > 0)) {
            for (PotionEffect eff : tipped.getCustomEffects()) {
                if (eff.getType() == PotionEffectType.POISON) {
                    type = "poison"; break;
                }
                if (eff.getType() == PotionEffectType.SLOWNESS && eff.getAmplifier() >= 5) {
                    type = "ice"; break;
                }
            }
            // 효과 없는 TippedArrow(폭탄) fallback
            if (type == null && tipped.getCustomEffects().isEmpty()) {
                type = "bomb";
            }
        }

        if (type == null) return;
        t2ArrowEntities.remove(arrow.getUniqueId()); // 정리

        Entity   hitEntity = event.getHitEntity();
        Location hitLoc    = event.getEntity().getLocation();
        World    world     = hitLoc.getWorld();

        if (hitEntity != null && hitEntity.equals(player)) return;

        // 타 직업 무기 사용 시 효과 지속시간도 함께 감소
        double pen = WeaponUtils.getPenaltyMultiplier(player, player.getInventory().getItemInMainHand(), plugin);

        switch (type) {
            case "poison" -> {
                if (hitEntity instanceof LivingEntity target) {
                    // 독 효과 직접 적용 (TippedArrow 바닐라 효과와 중복되어도 무관 — addPotionEffect는 갱신)
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (160 * pen), 0, false, true, true));
                    applyPoisonEffect(world, hitLoc);
                    player.sendMessage(ChatColor.DARK_GREEN + "[☠ 독 화살] " + ChatColor.WHITE
                            + target.getName() + " 독 적용! " + String.format("%.1f", 160 * pen / 20.0) + "초");
                }
                // 화살 제거 안 함 — 바닐라가 데미지 처리 후 화살을 박음
            }
            case "ice" -> {
                if (hitEntity instanceof LivingEntity target) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (100 * pen), 9, false, true, true));
                    applyIceEffect(world, hitLoc);
                    player.sendMessage(ChatColor.AQUA + "[❄ 얼음 화살] " + ChatColor.WHITE
                            + target.getName() + " 봉쇄! " + String.format("%.1f", 100 * pen / 20.0) + "초");
                }
            }
            case "bomb" -> {
                arrow.remove();
                applyBomb(player, hitLoc, world);
            }
            case "t1_quick" -> {
                arrow.remove();
                quickHitEffect(world, hitLoc);
            }
            case "t3_rain" -> {
                arrow.remove();
                rainHitEffect(world, hitLoc);
            }
        }
    }

    private void applyPoisonEffect(World world, Location loc) {
        world.spawnParticle(Particle.DUST, loc, 15, 0.35, 0.35, 0.35,
                new Particle.DustOptions(Color.fromRGB(40, 160, 40), 1.2f));
        world.spawnParticle(Particle.WITCH, loc, 8, 0.3, 0.4, 0.3, 0.05);
        world.spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.05);
        world.playSound(loc, Sound.ENTITY_SPIDER_HURT, 0.7f, 0.9f);
    }

    private void applyIceEffect(World world, Location loc) {
        world.spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.35, 0.35, 0.35, 0.05);
        world.spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3,
                new Particle.DustOptions(Color.fromRGB(120, 200, 255), 1.3f));
        world.spawnParticle(Particle.CLOUD, loc, 5, 0.2, 0.2, 0.2, 0.02);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.4f);
    }

    private void applyBomb(Player player, Location hitLoc, World world) {
        world.createExplosion(hitLoc, 2.5f, false, false, player);
        world.spawnParticle(Particle.EXPLOSION, hitLoc, 3, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.FLAME,     hitLoc, 20, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.SMOKE,     hitLoc, 15, 0.4, 0.4, 0.4, 0.05);
        world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        player.sendMessage(ChatColor.RED + "[💣 폭탄 화살] 폭발!");
    }

    private void quickHitEffect(World world, Location loc) {
        world.spawnParticle(Particle.CRIT, loc, 4, 0.2, 0.2, 0.2, 0.05);
        world.spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1,
                new Particle.DustOptions(Color.fromRGB(100, 255, 50), 1.0f));
    }

    private void rainHitEffect(World world, Location loc) {
        world.spawnParticle(Particle.CRIT, loc, 3, 0.1, 0.1, 0.1, 0.05);
        world.spawnParticle(Particle.DUST, loc, 4, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.fromRGB(80, 255, 30), 1.0f));
        world.playSound(loc, Sound.ENTITY_ARROW_HIT, 0.4f, 1.0f);
    }

    private void cooldownBar(Player player, long remainMs) {
        player.sendActionBar(Component.text("⏳ 쿨다운 " + (int) Math.ceil(remainMs / 1000.0) + "초")
                .color(NamedTextColor.RED));
    }
}
