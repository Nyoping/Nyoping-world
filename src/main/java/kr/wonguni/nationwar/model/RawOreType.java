package kr.wonguni.nationwar.model;

public enum RawOreType {
    DIAMOND("다이아 원석"),
    EMERALD("에메랄드 원석"),
    REDSTONE("레드스톤 원석"),
    LAPIS("청금석 원석"),
    QUARTZ("석영 원석"),
    AMETHYST("자수정 원석"),
    ANCIENT_DEBRIS("고대잔해 원석");

    private final String koName;
    RawOreType(String koName) { this.koName = koName; }
    public String koName() { return this.koName; }
}
