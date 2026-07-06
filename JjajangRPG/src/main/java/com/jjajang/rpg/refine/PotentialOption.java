package com.jjajang.rpg.refine;

public enum PotentialOption {
    HP              ("HP",              new int[]{2, 2, 4},        false, false),
    ATTACK          ("공격력",          new int[]{3, 6, 9},        true,  false),
    DAMAGE          ("데미지",          new int[]{5, 8, 12},       true,  false),
    CRIT_DAMAGE     ("크리티컬 데미지",  new int[]{4, 8, 12},       true,  false),
    CRIT_CHANCE     ("크리티컬 확률",    new int[]{5, 8, 12},       true,  false),
    BOSS_DAMAGE     ("보스몬스터 데미지", new int[]{5, 10, 15},      true,  false),
    MOB_DAMAGE      ("일반몬스터 데미지", new int[]{5, 10, 15},      true,  false),
    DEFENSE         ("방어력",          new int[]{100, 200, 350},  false, false),
    ACCURACY        ("명중률",          new int[]{5, 8, 12},       true,  false),
    GOLD_GAIN       ("골드 획득량",      new int[]{10, 20, 30},     true,  false),
    DROP_RATE       ("드랍률",          new int[]{10, 20, 30},     true,  false),
    MOVE_SPEED      ("이동속도",         new int[]{10, 15, 20},     true,  false),
    JUMP            ("점프력 (비활성화)", new int[]{1, 2, 3},        false, true),  // 버그 임시 비활성화
    LEVEL_REDUCE    ("착용레벨 감소",    new int[]{5, 10, 15},      false, false);

    public final String displayName;
    /** values[0]=에픽, [1]=유니크, [2]=레전드리 */
    public final int[] values;
    /** true = % 단위, false = 수치 단위 */
    public final boolean percent;
    /** true = 세공 롤에서 제외 (비활성화) */
    public final boolean disabled;

    PotentialOption(String displayName, int[] values, boolean percent, boolean disabled) {
        this.displayName = displayName;
        this.values = values;
        this.percent = percent;
        this.disabled = disabled;
    }

    public int getValue(PotentialGrade grade) {
        return values[grade.level];
    }

    public String formatValue(PotentialGrade grade) {
        int v = getValue(grade);
        if (this == HP) return "하트 " + (v / 2) + "칸 추가";
        return percent ? "+" + v + "%" : (this == LEVEL_REDUCE ? "-" + v : "+" + v);
    }

    /** 해당 등급에서 이 옵션이 롤 가능한지 */
    public boolean isAvailableFor(PotentialGrade grade) {
        if (disabled) return false;
        if (this == HP && grade == PotentialGrade.EPIC) return false;
        return true;
    }

    /** PDC 저장 형식: "ATTACK:5" */
    public String serialize(PotentialGrade grade) {
        return name() + ":" + getValue(grade);
    }

    public static PotentialOption fromKey(String key) {
        String type = key.contains(":") ? key.split(":")[0] : key;
        try { return valueOf(type); } catch (IllegalArgumentException e) { return null; }
    }

    public static int valueFromKey(String key) {
        if (!key.contains(":")) return 0;
        try { return Integer.parseInt(key.split(":")[1]); } catch (NumberFormatException e) { return 0; }
    }
}
