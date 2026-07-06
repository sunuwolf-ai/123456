package com.jjajang.rpg.reward;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RewardManager {

    // 보상 ID 상수
    public static final String REWARD_INITIAL        = "reward_initial";        // 기존 523G 보상 (하위 호환용, 미사용)
    public static final String REWARD_JJAJJANG_LAUNCH = "reward_jjajjang_launch"; // 짜장 출시기념 보상

    private final Map<String, Set<UUID>> claimed = new HashMap<>();
    private final File file;
    private final JavaPlugin plugin;

    public RewardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rewards.yml");
        load();
    }

    public boolean hasClaimed(UUID id, String rewardId) {
        return claimed.getOrDefault(rewardId, Collections.emptySet()).contains(id);
    }

    public void claim(UUID id, String rewardId) {
        claimed.computeIfAbsent(rewardId, k -> new HashSet<>()).add(id);
        save();
    }

    // 하위 호환: 기존 코드가 인자 없이 쓸 경우 → REWARD_INITIAL
    public boolean hasClaimed(UUID id) { return hasClaimed(id, REWARD_INITIAL); }
    public void claim(UUID id)         { claim(id, REWARD_INITIAL); }

    private void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // 신규 포맷: rewards.reward_initial, rewards.reward_nerf
        if (cfg.contains("rewards")) {
            for (String rewardId : cfg.getConfigurationSection("rewards").getKeys(false)) {
                Set<UUID> set = new HashSet<>();
                cfg.getStringList("rewards." + rewardId).forEach(s -> set.add(UUID.fromString(s)));
                claimed.put(rewardId, set);
            }
        } else if (cfg.contains("claimed")) {
            // 구버전 포맷 마이그레이션 (단일 목록 → REWARD_INITIAL)
            Set<UUID> set = new HashSet<>();
            cfg.getStringList("claimed").forEach(s -> set.add(UUID.fromString(s)));
            claimed.put(REWARD_INITIAL, set);
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Set<UUID>> entry : claimed.entrySet()) {
            cfg.set("rewards." + entry.getKey(),
                    entry.getValue().stream().map(UUID::toString).toList());
        }
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("보상 저장 실패: " + e.getMessage());
        }
    }
}
