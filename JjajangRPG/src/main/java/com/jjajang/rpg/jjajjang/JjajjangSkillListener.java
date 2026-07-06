package com.jjajang.rpg.jjajjang;

import com.jjajang.rpg.classes.ClassManager;
import com.jjajang.rpg.util.SkillDamageTracker;
import com.jjajang.rpg.util.WeaponUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class JjajjangSkillListener implements Listener {

    private static final long CD_T1    = 7_000L;
    private static final long CD_T2    = 10_000L;
    private static final long CD_T3    = 20_000L;
    private static final long BASIC_CD = 450L;
    private static final int  BOWL_HEAL = 3; // 짜장 그릇 회수 시 회복량 (1.5하트)

    private final Map<UUID, Long> t1Used    = new HashMap<>();
    private final Map<UUID, Long> t2Used    = new HashMap<>();
    private final Map<UUID, Long> t3Used    = new HashMap<>();
    private final Map<UUID, Long> basicUsed = new HashMap<>();

    private final JavaPlugin plugin;

    public JjajjangSkillListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── 우클릭: 티어 스킬 ────────────────────────────────────────────
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!JjajjangItems.isJjajjangWeapon(held, plugin)) return;

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
        if (!JjajjangItems.isJjajjangWeapon(held, plugin)) return;

        event.setCancelled(true);
        handleBasicAttack(player, held);
    }

    // ── 좌클릭(적 직접 타격): 바닐라 데미지 무효화 + 기본공격 대체 ──────
    @EventHandler(priority = EventPriority.LOW)
    public void onMeleeAttempt(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (SkillDamageTracker.isSkill(player.getUniqueId())) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!JjajjangItems.isJjajjangWeapon(held, plugin)) return;
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
                new Particle.DustOptions(Color.fromRGB(160, 110, 40), 1.0f));
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 1.3f);
    }

    // ── 기본공격: 약한 에너지볼 ───────────────────────────────────────
    private void handleBasicAttack(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - basicUsed.getOrDefault(id, 0L) < BASIC_CD) return;
        basicUsed.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 3.0 * pen;
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.6f, 1.6f);

        launchOrb(player, 1.1, 16.0, 0.9,
                loc -> world.spawnParticle(Particle.CRIT, loc, 2, 0.05, 0.05, 0.05, 0),
                (target, loc) -> {
                    SkillDamageTracker.mark(id);
                    target.damage(dmg, player);
                    SkillDamageTracker.clear(id);
                    world.spawnParticle(Particle.CRIT, loc, 6, 0.2, 0.2, 0.2, 0.05);
                    world.spawnParticle(Particle.DUST, loc, 8, 0.2, 0.2, 0.2,
                            new Particle.DustOptions(Color.fromRGB(160, 110, 40), 1.0f));
                    world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 1.3f);
                });
    }

    // ── T1: 짜장소환 (대상 머리 위에 소환해 떨어뜨림) ──────────────────
    private void handleT1(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T1 - (now - t1Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }

        LivingEntity target = findTarget(player, 16.0);
        if (target == null) {
            player.sendActionBar(Component.text("대상이 없습니다!").color(NamedTextColor.RED));
            return;
        }
        t1Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double dmg = 12.0 * pen;
        World world = player.getWorld();
        player.sendActionBar(Component.text("🥣 짜장소환!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        world.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 0.8f);

        Location above = target.getLocation().add(0, 3, 0);
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick >= 8 || !target.isValid()) {
                    if (target.isValid()) impact(player, id, target, dmg, world);
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.DUST, above, 3, 0.15, 0.15, 0.15,
                        new Particle.DustOptions(Color.fromRGB(160, 110, 40), 1.2f));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void impact(Player player, UUID id, LivingEntity target, double dmg, World world) {
        SkillDamageTracker.mark(id);
        target.damage(dmg, player);
        SkillDamageTracker.clear(id);

        Location loc = target.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.DUST, loc, 20, 0.3, 0.4, 0.3,
                new Particle.DustOptions(Color.fromRGB(120, 80, 30), 1.4f));
        world.spawnParticle(Particle.SPLASH, loc, 10, 0.3, 0.2, 0.3, 0.05);
        world.playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.7f);
        player.sendMessage(ChatColor.GOLD + "[짜장소환] " + ChatColor.WHITE + target.getName()
                + " " + ChatColor.RED + String.format("%.1f", dmg) + " 피해");

        dropBowl(world, loc, id);
    }

    // ── T2: 짜장면 투척 (춘장 묻혀 지속 피해) ──────────────────────────
    private void handleT2(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T2 - (now - t2Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t2Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        double initDmg = 5.0 * pen;
        double tickDmg = 2.0 * pen;
        int    ticks   = Math.max(1, (int) Math.round(4 * pen));
        World world = player.getWorld();
        player.sendActionBar(Component.text("🍜 짜장면 투척!").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        world.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 0.8f);

        launchOrb(player, 1.0, 20.0, 1.0,
                loc -> world.spawnParticle(Particle.DUST, loc, 3, 0.08, 0.08, 0.08,
                        new Particle.DustOptions(Color.fromRGB(90, 60, 20), 1.1f)),
                (target, loc) -> {
                    SkillDamageTracker.mark(id);
                    target.damage(initDmg, player);
                    SkillDamageTracker.clear(id);
                    world.spawnParticle(Particle.DUST, loc, 16, 0.3, 0.3, 0.3,
                            new Particle.DustOptions(Color.fromRGB(90, 60, 20), 1.4f));
                    world.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.7f);
                    player.sendMessage(ChatColor.DARK_PURPLE + "[명품 레스토랑] " + ChatColor.WHITE
                            + target.getName() + " 춘장 적중! " + ChatColor.RED + String.format("%.1f", initDmg) + " 피해");
                    dropBowl(world, loc, id);

                    new BukkitRunnable() {
                        int tick = 0;
                        @Override public void run() {
                            if (tick >= ticks || !target.isValid()) { this.cancel(); return; }
                            SkillDamageTracker.mark(id);
                            target.damage(tickDmg, player);
                            SkillDamageTracker.clear(id);
                            world.spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2,
                                    new Particle.DustOptions(Color.fromRGB(90, 60, 20), 1.0f));
                            tick++;
                        }
                    }.runTaskTimer(plugin, 20L, 20L);
                });
    }

    // ── T3: 준우준우화 (광역 독+이속감소, 갑옷 한 피스 탈락) ───────────
    private void handleT3(Player player, ItemStack held) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long rem = CD_T3 - (now - t3Used.getOrDefault(id, 0L));
        if (rem > 0) { cooldownBar(player, rem); return; }
        t3Used.put(id, now);

        double pen = WeaponUtils.getPenaltyMultiplier(player, held, plugin);
        World world = player.getWorld();
        player.sendActionBar(Component.text("🍱 준우준우화!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
        world.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 0.5f);

        double radius = 5.0;
        Location center = player.getLocation();
        world.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 40, radius * 0.5, 1.0, radius * 0.5,
                new Particle.DustOptions(Color.fromRGB(200, 160, 40), 1.5f));

        int poisonTicks = (int) (100 * pen);
        int slowTicks   = (int) (100 * pen);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity target) || e.equals(player)) continue;

            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.POISON, poisonTicks, 0, false, true, true));
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, slowTicks, 1, false, true, true));

            Location loc = target.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.DUST, loc, 14, 0.3, 0.4, 0.3,
                    new Particle.DustOptions(Color.fromRGB(200, 160, 40), 1.3f));
            world.playSound(loc, Sound.ENTITY_VILLAGER_HURT, 0.8f, 0.6f);

            stripRandomArmor(target);

            player.sendMessage(ChatColor.DARK_RED + "[준우준우화] " + ChatColor.WHITE + target.getName()
                    + ChatColor.GRAY + " 독+이속감소 적용, 갑옷 탈락!");
        }
    }

    private void stripRandomArmor(LivingEntity target) {
        EntityEquipment eq = target.getEquipment();
        if (eq == null) return;

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        java.util.List<EquipmentSlot> filled = new java.util.ArrayList<>();
        for (EquipmentSlot s : slots) {
            ItemStack piece = eq.getItem(s);
            if (piece != null && !piece.getType().isAir()) filled.add(s);
        }
        if (filled.isEmpty()) return;

        EquipmentSlot pick = filled.get((int) (Math.random() * filled.size()));
        ItemStack removed = eq.getItem(pick);
        eq.setItem(pick, null);

        if (target instanceof Player p) {
            java.util.Map<Integer, ItemStack> overflow = p.getInventory().addItem(removed);
            overflow.values().forEach(o -> p.getWorld().dropItemNaturally(p.getLocation(), o));
        } else {
            target.getWorld().dropItemNaturally(target.getLocation(), removed);
        }
    }

    // ── 패시브: 스킬 명중 시 짜장 그릇 드랍 (본인만 회수 가능) ────────
    private void dropBowl(World world, Location loc, UUID owner) {
        world.dropItem(loc, JjajjangBowlItem.create(owner));
    }

    @EventHandler
    public void onBowlPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack itemStack = event.getItem().getItemStack();
        if (!JjajjangBowlItem.is(itemStack)) return;

        if (!JjajjangBowlItem.isOwner(itemStack, player.getUniqueId())) {
            event.setCancelled(true); // 본인 그릇이 아니면 줍지 못함
            return;
        }

        event.setCancelled(true);
        event.getItem().remove();

        var maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxHp != null ? maxHp.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + BOWL_HEAL));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 4, 0.3, 0.2, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
        player.sendActionBar(Component.text("🥣 짜장 그릇 회수! +" + BOWL_HEAL + " 체력").color(NamedTextColor.GOLD));
    }

    // ── 공용 유틸 ────────────────────────────────────────────────────
    private LivingEntity findTarget(Player player, double range) {
        LivingEntity best = null;
        double bestScore = -1;
        Vector eyeDir = player.getEyeLocation().getDirection().normalize();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof LivingEntity le) || e.equals(player)) continue;
            Vector toEntity = le.getLocation().add(0, 1, 0).subtract(player.getEyeLocation()).toVector();
            double dist = toEntity.length();
            if (dist < 0.01) continue;
            toEntity.normalize();
            double dot = eyeDir.dot(toEntity);
            if (dot > 0.85 && dot > bestScore) { bestScore = dot; best = le; }
        }
        return best;
    }

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
