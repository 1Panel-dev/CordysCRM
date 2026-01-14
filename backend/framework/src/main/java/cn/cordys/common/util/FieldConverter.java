package cn.cordys.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

public class FieldConverter {

    public static String toCamelCaseWithCommons(String snakeCase) {
        if (StringUtils.isBlank(snakeCase)) {
            return snakeCase;
        }

        return CaseUtils.toCamelCase(snakeCase.toLowerCase(), false, '_');
    }
}
