/*
 * Decompiled with CFR 0.152.
 */
package kr.wonguni.nationwar.model;

public enum JobType {
    UNEMPLOYED,
    MINER,
    FARMER,
    COOK,
    FISHER,
    HUNTER,
    BREWER;


    public static JobType from(String s) {
        if (s == null) {
            return UNEMPLOYED;
        }
        try {
            return JobType.valueOf(s.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return UNEMPLOYED;
        }
    }
}

