package com.jjajang.rpg.gold;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoldManager {
    private final Map<UUID, Long> gold = new HashMap<>();
    private final File file;
    private final JavaPlugin plugin;

    public GoldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gold.yml");
        load();
    }

    public long get(UUID id) { return gold.getOrDefault(id, 0L); }

    public void add(UUID id, long amount) {
        gold.put(id, get(id) + amount);
        save();
    }

    public boolean spend(UUID id, long amount) {
        if (get(id) < amount) return false;
        gold.put(id, get(id) - amount);
        save();
        return true;
    }

    public void set(UUID id, long amount) {
        gold.put(id, Math.max(0, amount));
        save();
    }

    private void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.contains("gold")) {
            cfg.getConfigurationSection("gold").getKeys(false)
               .forEach(k -> gold.put(UUID.fromString(k), cfg.getLong("gold." + k)));
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        gold.forEach((id, g) -> cfg.set("gold." + id, g));
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("골드 저장 실패: " + e.getMessage());
        }
    }
}
