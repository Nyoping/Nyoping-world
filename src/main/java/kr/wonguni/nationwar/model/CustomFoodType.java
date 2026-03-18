package kr.wonguni.nationwar.model;

import java.util.Locale;

public enum CustomFoodType {
    // Placeholder list (문서 기반으로 나중에 확장)
    STEW("스튜"),
    SANDWICH("샌드위치"),
    SALAD("샐러드"),
    SKEWER("꼬치"),
    SOUP("수프"),
    DESSERT("디저트");

    private final String koName;

    CustomFoodType(String koName) {
        this.koName = koName;
    }

    public String koName() {
        return this.koName;
    }

    public static CustomFoodType fromId(String id) {
        if (id == null) return null;
        try {
            return CustomFoodType.valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
}
