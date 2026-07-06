package com.jjajang.rpg.util;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 스킬 피해와 일반 공격 피해를 구분하기 위한 트래커 */
public class SkillDamageTracker {

    private static final Set<UUID> active = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void mark(UUID id)    { active.add(id); }
    public static void clear(UUID id)   { active.remove(id); }
    public static boolean isSkill(UUID id) { return active.contains(id); }
}
