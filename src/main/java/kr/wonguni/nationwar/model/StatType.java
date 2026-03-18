/*
 * Decompiled with CFR 0.152.
 */
package kr.wonguni.nationwar.model;

public enum StatType {
    POWER("\uacf5\uaca9"),
    HUNGER("\ubc30\uace0\ud514"),
    HEALTH("\uccb4\ub825");

    private final String kr;

    private StatType(String kr) {
        this.kr = kr;
    }

    public String koreanName() {
        return this.kr;
    }

    public static StatType fromString(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toUpperCase();
        for (StatType st : StatType.values()) {
            if (!st.name().equals(t)) continue;
            return st;
        }
        switch (s.trim()) {
            case "\uacf5\uaca9": {
                return POWER;
            }
            case "\ud5c8\uae30": {
                return HUNGER;
            }
            case "\ubc30\uace0\ud514": {
                return HUNGER;
            }
            case "\uccb4\ub825": {
                return HEALTH;
            }
        }
        return null;
    }
}

