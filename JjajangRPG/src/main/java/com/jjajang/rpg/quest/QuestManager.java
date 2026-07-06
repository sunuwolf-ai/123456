package com.jjajang.rpg.quest;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestManager {

    private final Map<UUID, Integer> huskKills = new HashMap<>();
    private final Map<UUID, String> huskDates = new HashMap<>(); // date when husk quest was last completed

    private final File file;
    private final JavaPlugin plugin;

    public static final int HUSK_GOAL = 5;

    public QuestManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        load();
    }

    // ── 일일 퀘스트: 허스크 ──────────────────────────────────────────────

    public int getHuskKills(UUID id) { return huskKills.getOrDefault(id, 0); }

    /** Returns true if the daily husk quest is already completed today */
    public boolean isHuskDoneToday(UUID id) {
        String stored = huskDates.getOrDefault(id, "");
        return stored.equals(today());
    }

    /** Adds a husk kill. Returns true if the quest just completed. */
    public boolean addHuskKill(UUID id) {
        if (isHuskDoneToday(id)) return false;
        int kills = huskKills.getOrDefault(id, 0) + 1;
        huskKills.put(id, kills);
        if (kills >= HUSK_GOAL) {
            huskDates.put(id, today());
            huskKills.put(id, 0);
            save();
            return true;
        }
        save();
        return false;
    }

    /** Milliseconds until midnight (next daily reset) */
    public long msUntilReset() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, midnight).toMillis();
    }

    public String timeUntilReset() {
        long ms = msUntilReset();
        long h = ms / 3_600_000;
        long m = (ms % 3_600_000) / 60_000;
        long s = (ms % 60_000) / 1_000;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String today() { return LocalDate.now().toString(); }

    // ── 저장/불러오기 ────────────────────────────────────────────────────

    private void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.contains("husk_kills")) {
            cfg.getConfigurationSection("husk_kills").getKeys(false)
               .forEach(k -> huskKills.put(UUID.fromString(k), cfg.getInt("husk_kills." + k)));
        }
        if (cfg.contains("husk_dates")) {
            cfg.getConfigurationSection("husk_dates").getKeys(false)
               .forEach(k -> huskDates.put(UUID.fromString(k), cfg.getString("husk_dates." + k)));
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        huskKills.forEach((id, v) -> cfg.set("husk_kills." + id, v));
        huskDates.forEach((id, v) -> cfg.set("husk_dates." + id, v));
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("퀘스트 저장 실패: " + e.getMessage());
        }
    }
}
