package kr.wonguni.nationwar.model;

import java.util.Locale;

public enum CustomFoodType {
    // --- 최종요리 (등급 판정 대상) ---
    PIZZA("피자", true),
    SALT_BREAD("소금빵", true),
    COOKIE("쿠키", true),
    STEAK_SET("스테이크 정식", true),
    JEYUK("제육볶음", true),
    KIMCHI_JEYUK("김치제육볶음", true),
    CAKE("케이크", true),
    STRAWBERRY_CHOCO_CAKE("딸기초코케이크", true),
    PUMPKIN_PIE("호박파이", true),
    BEER("맥주", true),
    BEEF_CURRY_RICE("소고기카레라이스", true),
    CHICKEN_CURRY_RICE("치킨카레라이스", true),
    BEER_CHICKEN("비어치킨", true),
    MUSHROOM_STEW("버섯스튜", true),
    BEET_SOUP("비트스프", true),
    SALMON_BOWL("연어덮밥", true),
    COD_SOUP("대구탕", true),
    RABBIT_STEW("토끼스튜", true),
    HWACHAE("화채양동이", true),
    YANGKKOCHI("양꼬치", true),
    KIMCHI_JJIGAE("김치찌개", true),
    DOENJANG_JJIGAE("된장찌개", true),
    HAMBURGER("햄버거", true),
    KIMCHI_FRIED_RICE("김치볶음밥", true),
    CHICKEN_NUGGET("치킨너겟", true),
    FRENCH_FRIES("감자튀김", true),
    JUMEOKBAP("주먹밥", true),

    // --- 서브/비등급 판매 가능 ---
    GRILLED_CARROT("구운 당근", false),
    TROPICAL_FISH_GRILLED("열대어 구이", false),
    PUFFERFISH_SASHIMI("복어회", false),
    SALMON_SASHIMI("연어회", false),
    COD_SASHIMI("대구회", false),

    // --- 재료 (판매 불가) ---
    EGG_FRY("계란후라이", false),
    MALT("맥아", false),
    BAD_KIMCHI_RICE("아쉬운 김치볶음밥", false),
    BUTTER("버터", false),
    CREAM("생크림", false),
    CHEESE("치즈", false),
    TOFU("두부", false),
    RICE("밥", false),
    MEJU("메주", false),
    BONE_BROTH("사골육수양동이", false),
    DOENJANG("된장", false),
    GOCHUJANG("고추장", false),
    SALT("소금", false),
    CHILI_POWDER("고춧가루", false),
    DOUGH("반죽", false),
    COOKIE_DOUGH("쿠키반죽", false),
    BREAD_DOUGH("빵반죽", false),
    PIZZA_DOUGH("피자반죽", false),
    PUMPKIN_PIE_DOUGH("호박파이반죽", false),
    KIMCHI("김치", false),
    YEAST("이스트", false),
    WORT("맥즙", false),
    ROASTED_COCOA("로스팅된 코코아 콩", false),
    SLICED_POTATO("썰린 감자", false),
    POLISHED_RICE("쌀", false),
    BEEF_CURRY("소고기카레", false),
    CHICKEN_CURRY("치킨카레", false);

    private final String koName;
    private final boolean finalDish;

    CustomFoodType(String koName, boolean finalDish) {
        this.koName = koName;
        this.finalDish = finalDish;
    }

    public String koName() {
        return this.koName;
    }

    /** Whether this is a final dish (등급 판정 대상). */
    public boolean isFinalDish() {
        return this.finalDish;
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
