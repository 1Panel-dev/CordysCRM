package io.cordys.common.util;

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-11  10:58
 */
public class CaseFormatUtils {
    /**
     * 将驼峰字符串转换为下划线分隔的字符串。
     *
     * @param camelStr 驼峰字符串
     * @return 下划线字符串
     */
    public static String camelToUnderscore(String camelStr) {
        return convertCamelToSeparator(camelStr, '_');
    }

    /**
     * 将驼峰字符串转换为指定分隔符的字符串。
     *
     * @param camelStr  驼峰字符串
     * @param separator 分隔符
     * @return 分隔符字符串
     */
    public static String convertCamelToSeparator(String camelStr, char separator) {
        if (camelStr == null || camelStr.trim().isEmpty()) {
            return camelStr;
        }
        StringBuilder result = new StringBuilder();
        char[] chars = camelStr.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append(separator);
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 将字符串首字母转换为大写
     * @param input 输入字符串
     * @return 首字母大写的字符串
     */
    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 提取第一个字符并转换为大写
        char firstChar = Character.toUpperCase(input.charAt(0));

        // 如果字符串只有一个字符，直接返回
        if (input.length() == 1) {
            return String.valueOf(firstChar);
        }

        // 拼接剩余字符
        return firstChar + input.substring(1);
    }
}
