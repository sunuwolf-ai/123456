package com.jjajang.rpg.event;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EventManager implements Listener {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // 레이드 이벤트 마일스톤 (처치 횟수)
    public static final int[] RAID_MILESTONES = {2, 4, 6, 8, 10};

    // 짜장패스 티어별 필요 처치 횟수 (0-indexed: 티어 N = PASS_KILLS[N-1])
    public static final int[] PASS_KILLS = {0, 1, 3, 5, 8, 10, 15, 20, 25, 30};

    // 패스 프리미엄 가격
    public static final long PASS_PRICE = 149_000L;

    // 출석 보상 (캐시, day 1~7)
    public static final long[] ATTENDANCE_REWARDS = {1000, 1500, 2000, 2500, 3000, 3500, 5000};

    private final Map<UUID, Integer>     bossKills          = new HashMap<>();
    private final Map<UUID, Set<Integer>>raidClaimed        = new HashMap<>();
    private final Map<UUID, String>      attendanceLastDate = new HashMap<>();
    private final Map<UUID, Integer>     attendanceDayInCycle = new HashMap<>(); // 마지막 수령 day(1-7), 0=미수령
    private final Set<UUID>              passPremium        = new HashSet<>();
    private final Map<UUID, Set<Integer>>passFreeClaimed    = new HashMap<>();
    private final Map<UUID, Set<Integer>>passPremClaimed    = new HashMap<>();

    private final NamespacedKey bossKey;
    private final File file;
    private final JavaPlugin plugin;

    public EventManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "boss_kimsh");
        this.file = new File(plugin.getDataFolder(), "events.yml");
        load();
    }

    // ── 보스 처치 ─────────────────────────────────────────────────
    public int getBossKills(UUID id)  { return bossKills.getOrDefault(id, 0); }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        if (!le.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        bossKills.put(killer.getUniqueId(), getBossKills(killer.getUniqueId()) + 1);
        save();
    }

    // ── 레이드 이벤트 ─────────────────────────────────────────────
    public boolean isRaidClaimed(UUID id, int milestone) {
        return raidClaimed.getOrDefault(id, Collections.emptySet()).contains(milestone);
    }
    public void claimRaid(UUID id, int milestone) {
        raidClaimed.computeIfAbsent(id, k -> new HashSet<>()).add(milestone);
        save();
    }

    // ── 출석 이벤트 ───────────────────────────────────────────────
    public boolean canClaimAttendance(UUID id) {
        String today = LocalDate.now().format(DATE_FMT);
        return !today.equals(attendanceLastDate.getOrDefault(id, ""));
    }

    public int getLastClaimedDay(UUID id) {
        return attendanceDayInCycle.getOrDefault(id, 0);
    }

    public long claimAttendance(UUID id) {
        int nextDay = (getLastClaimedDay(id) % 7) + 1;
        long reward = ATTENDANCE_REWARDS[nextDay - 1];
        attendanceDayInCycle.put(id, nextDay);
        attendanceLastDate.put(id, LocalDate.now().format(DATE_FMT));
        save();
        return reward;
    }

    // ── 짜장패스 ──────────────────────────────────────────────────
    public boolean hasPremiumPass(UUID id)   { return passPremium.contains(id); }
    public void purchasePremiumPass(UUID id) { passPremium.add(id); save(); }

    public boolean isPassFreeClaimed(UUID id, int tier) {
        return passFreeClaimed.getOrDefault(id, Collections.emptySet()).contains(tier);
    }
    public boolean isPassPremClaimed(UUID id, int tier) {
        return passPremClaimed.getOrDefault(id, Collections.emptySet()).contains(tier);
    }
    public void claimPassFree(UUID id, int tier) {
        passFreeClaimed.computeIfAbsent(id, k -> new HashSet<>()).add(tier);
        save();
    }
    public void claimPassPrem(UUID id, int tier) {
        passPremClaimed.computeIfAbsent(id, k -> new HashSet<>()).add(tier);
        save();
    }

    // 티어 잠금 해제 여부 (boss kills >= 해당 티어 요구치)
    public boolean isTierUnlocked(UUID id, int tier) {
        return getBossKills(id) >= PASS_KILLS[tier - 1];
    }

    // ── 저장/로드 ─────────────────────────────────────────────────
    private void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (cfg.isConfigurationSection("boss_kills")) {
            for (String k : cfg.getConfigurationSection("boss_kills").getKeys(false))
                bossKills.put(UUID.fromString(k), cfg.getInt("boss_kills." + k));
        }
        if (cfg.isConfigurationSection("raid_claimed")) {
            for (String k : cfg.getConfigurationSection("raid_claimed").getKeys(false)) {
                Set<Integer> s = new HashSet<>();
                cfg.getIntegerList("raid_claimed." + k).forEach(s::add);
                raidClaimed.put(UUID.fromString(k), s);
            }
        }
        if (cfg.isConfigurationSection("attendance")) {
            for (String k : cfg.getConfigurationSection("attendance").getKeys(false)) {
                UUID id = UUID.fromString(k);
                attendanceLastDate.put(id, cfg.getString("attendance." + k + ".last_date", ""));
                attendanceDayInCycle.put(id, cfg.getInt("attendance." + k + ".day", 0));
            }
        }
        if (cfg.contains("pass_premium"))
            cfg.getStringList("pass_premium").forEach(s -> passPremium.add(UUID.fromString(s)));
        if (cfg.isConfigurationSection("pass_free_claimed")) {
            for (String k : cfg.getConfigurationSection("pass_free_claimed").getKeys(false)) {
                Set<Integer> s = new HashSet<>();
                cfg.getIntegerList("pass_free_claimed." + k).forEach(s::add);
                passFreeClaimed.put(UUID.fromString(k), s);
            }
        }
        if (cfg.isConfigurationSection("pass_prem_claimed")) {
            for (String k : cfg.getConfigurationSection("pass_prem_claimed").getKeys(false)) {
                Set<Integer> s = new HashSet<>();
                cfg.getIntegerList("pass_prem_claimed." + k).forEach(s::add);
                passPremClaimed.put(UUID.fromString(k), s);
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        bossKills.forEach((id, v) -> cfg.set("boss_kills." + id, v));
        raidClaimed.forEach((id, s) -> cfg.set("raid_claimed." + id, new ArrayList<>(s)));
        attendanceLastDate.forEach((id, d) -> {
            cfg.set("attendance." + id + ".last_date", d);
            cfg.set("attendance." + id + ".day", attendanceDayInCycle.getOrDefault(id, 0));
        });
        cfg.set("pass_premium", passPremium.stream().map(UUID::toString).toList());
        passFreeClaimed.forEach((id, s) -> cfg.set("pass_free_claimed." + id, new ArrayList<>(s)));
        passPremClaimed.forEach((id, s) -> cfg.set("pass_prem_claimed." + id, new ArrayList<>(s)));
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("이벤트 데이터 저장 실패: " + e.getMessage());
        }
    }
}
