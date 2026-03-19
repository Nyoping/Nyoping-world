/*
 * Decompiled with CFR 0.152.
 */
package kr.wonguni.nationwar.util;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeUtil() {
    }

    public static ZonedDateTime nowKst() {
        return ZonedDateTime.now(KST);
    }

    public static ZonedDateTime nextKstMidnight() {
        ZonedDateTime now = TimeUtil.nowKst();
        ZonedDateTime next = now.truncatedTo(ChronoUnit.DAYS).plusDays(1L);
        return next;
    }

    public static long millisUntil(ZonedDateTime future) {
        return Duration.between(TimeUtil.nowKst(), future).toMillis();
    }
}

