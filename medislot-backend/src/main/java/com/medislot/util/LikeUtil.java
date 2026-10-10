package com.medislot.util;

import java.util.Locale;

public final class LikeUtil {

    private LikeUtil() {}

    /** Lower-case "%text%" pattern using '!' as the escape character, or null when blank. */
    public static String contains(String text) {
        if (text == null || text.isBlank()) return null;
        String t = text.trim();
        if (t.length() > 100) t = t.substring(0, 100);
        return "%" + t.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}