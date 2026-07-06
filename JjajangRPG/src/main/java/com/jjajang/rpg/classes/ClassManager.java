package com.jjajang.rpg.classes;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClassManager {

    private static final Map<UUID, PlayerClass> playerClasses = new HashMap<>();
    private static NamespacedKey CLASS_KEY;

    public static void init(JavaPlugin plugin) {
        CLASS_KEY = new NamespacedKey(plugin, "player_class");
    }

    public static PlayerClass getClass(Player player) {
        return playerClasses.getOrDefault(player.getUniqueId(), PlayerClass.NONE);
    }

    public static boolean hasClass(Player player) {
        return getClass(player) != PlayerClass.NONE;
    }

    public static void setClass(Player player, PlayerClass cls) {
        PlayerClass previous = getClass(player);
        playerClasses.put(player.getUniqueId(), cls);
        // PDC에 저장 (재접속 시 복원용)
        player.getPersistentDataContainer().set(CLASS_KEY, PersistentDataType.STRING, cls.name());
        applyPassives(player, previous, cls);
    }

    /** 접속 시 PDC에서 클래스 로드 후 패시브 적용 */
    public static void loadAndApply(Player player) {
        String saved = player.getPersistentDataContainer().get(CLASS_KEY, PersistentDataType.STRING);
        PlayerClass cls = PlayerClass.NONE;
        if (saved != null) {
            try { cls = PlayerClass.valueOf(saved); } catch (IllegalArgumentException ignored) {}
        }
        playerClasses.put(player.getUniqueId(), cls);

        if (cls.isWarrior()) {
            applyWarriorPassive(player);
        } else {
            // 전사가 아니면 무조건 기본 체력으로 리셋 (HP 버그 방지)
            resetDefaultHealth(player);
        }
    }

    private static void applyPassives(Player player, PlayerClass previous, PlayerClass next) {
        if (previous.isWarrior()) removeWarriorPassive(player);
        if (next.isWarrior())     applyWarriorPassive(player);
        // 전사 → 전사 아님으로 바뀔 때 확실하게 리셋
        if (!next.isWarrior())    resetDefaultHealth(player);
    }

    public static void applyWarriorPassive(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) attr.setBaseValue(40.0);
        player.setHealth(Math.min(player.getHealth() + 20, 40.0));
    }

    private static void removeWarriorPassive(Player player) {
        resetDefaultHealth(player);
    }

    private static void resetDefaultHealth(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) player.setHealth(20.0);
        }
    }

    public static void remove(UUID uuid) {
        playerClasses.remove(uuid);
    }
}
