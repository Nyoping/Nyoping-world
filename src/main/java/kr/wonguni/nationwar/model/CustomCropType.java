package kr.wonguni.nationwar.model;

import java.util.Locale;

public enum CustomCropType {
    CHILI("고추"),
    GREEN_ONION("파"),
    GARLIC("마늘"),
    ONION("양파"),
    CABBAGE("배추"),
    RADISH("무"),
    SOYBEAN("콩"),
    BARLEY("보리"),
    HOP("홉"),
    RICE("벼"),
    STRAWBERRY("딸기"),
    TOMATO("토마토"),
    TURMERIC("강황");

    private final String koName;

    CustomCropType(String koName) {
        this.koName = koName;
    }

    public String koName() {
        return this.koName;
    }

    public static CustomCropType fromId(String id) {
        if (id == null) return null;
        try {
            return CustomCropType.valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
}
