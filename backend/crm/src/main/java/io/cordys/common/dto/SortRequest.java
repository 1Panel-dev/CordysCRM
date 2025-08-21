package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * @author jianxing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortRequest {

    @Pattern(regexp = "^[A-Za-z0-9]+$")
    @Schema(description = "排序字段")
    private String name;

    @Schema(description = "排序类型(asc/desc)")
    private String type;

    public String getName() {
        if (checkSqlInjection(name)) {
            return "1";
        }
        return camelToUnderline(name);
    }

    public String getType() {
        if (Strings.CI.equals(type, "asc")) {
            return "asc";
        } else {
            return "desc";
        }
    }

    /**
     * mapper 中调用
     *
     * @return
     */
    public boolean valid() {
        return StringUtils.isNotBlank(name) && !checkSqlInjection(name);
    }

    public static String camelToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        // 使用正则表达式将驼峰转换为下划线
        String underline = camelCase.replaceAll("([A-Z])", "_$1").toLowerCase();

        // 如果开头有下划线，去掉
        if (underline.startsWith("_")) {
            underline = underline.substring(1);
        }

        return underline;
    }

    /**
     * 返回 true 表示存在 SQL 注入风险
     *
     * @param script
     * @return
     */
    public static boolean checkSqlInjection(String script) {
        if (StringUtils.isEmpty(script)) {
            return false;
        }
        // 检测危险SQL模式
        java.util.regex.Pattern dangerousPattern = java.util.regex.Pattern.compile(
                "(;|--|#|'|\"|/\\*|\\*/|\\b(select|insert|update|delete|drop|alter|truncate|exec|union|xp_)\\b)",
                java.util.regex.Pattern.CASE_INSENSITIVE);

        // 返回true表示存在注入风险
        return dangerousPattern.matcher(script).find();
    }
}
