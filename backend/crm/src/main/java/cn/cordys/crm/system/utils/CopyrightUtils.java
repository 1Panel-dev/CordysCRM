package cn.cordys.crm.system.utils;

public class CopyrightUtils {
    private static final int START_YEAR = 2014;

    public static String getCopyright(int currentYear) {
        if (START_YEAR == currentYear) {
            return String.format("Copyright © %d 飞致云", currentYear);
        } else {
            return String.format("Copyright © %d-%d 飞致云", START_YEAR, currentYear);
        }
    }
}
