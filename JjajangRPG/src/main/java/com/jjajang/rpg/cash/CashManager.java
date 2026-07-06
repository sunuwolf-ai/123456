package com.jjajang.rpg.cash;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CashManager {
    private final Map<UUID, Long> cash = new HashMap<>();
    private final File file;
    private final JavaPlugin plugin;

    public CashManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cash.yml");
        load();
    }

    public long get(UUID id) { return cash.getOrDefault(id, 0L); }

    public void add(UUID id, long amount) {
        cash.put(id, get(id) + amount);
        save();
    }

    public boolean spend(UUID id, long amount) {
        if (get(id) < amount) return false;
        cash.put(id, get(id) - amount);
        save();
        return true;
    }

    public void set(UUID id, long amount) {
        cash.put(id, Math.max(0, amount));
        save();
    }

    private void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.contains("cash")) {
            cfg.getConfigurationSection("cash").getKeys(false)
               .forEach(k -> cash.put(UUID.fromString(k), cfg.getLong("cash." + k)));
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cash.forEach((id, c) -> cfg.set("cash." + id, c));
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("캐시 저장 실패: " + e.getMessage());
        }
    }
}
