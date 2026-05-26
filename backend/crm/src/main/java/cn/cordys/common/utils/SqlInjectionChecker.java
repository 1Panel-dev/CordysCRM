package cn.cordys.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class SqlInjectionChecker {

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "--[^\\n]*|#|/\\*!?|\\*/" +           // SQL 注释
                    "|'|\"|\\\\|;" +                      // 危险分隔符
                    "|\\b(?:xp_\\w+|sp_\\w+|dbms_\\w+|utl_\\w+)\\b" +  // 危险存储过程
                    "|\\b(?:select|insert|update|delete|drop|alter|truncate|exec|execute|merge|replace|load_file|outfile|dumpfile)\\b" +
                    "|\\b(?:union\\s+(?:all\\s+)?select|information_schema|table_name|column_name)\\b" +
                    "|\\b(?:sleep|benchmark|pg_sleep|dbms_lock\\.sleep|dbms_pipe\\.receive_message|waitfor\\s+delay)\\b" +
                    "|\\b(?:and|or)\\b\\s*(?:'|[0-9]|\\()",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * 检测输入字符串是否包含可疑的 SQL 注入特征。
     *
     * @param input 待检测的用户输入
     *
     * @return true 表示可能存在注入风险
     */
    public static boolean containsSqlInjectionRisk(String input) {
        if (StringUtils.isEmpty(input)) {
            return false;
        }
        // 归一化处理：尝试解码常见编码绕过（如双重URL编码、Unicode编码等）
        String normalized = normalize(input);

        return DANGEROUS_PATTERN.matcher(normalized).find();
    }

    // 简单归一化：解码URL编码、反引号等，防止编码绕过
    private static String normalize(String input) {
        try {
            // 解码URL编码 (%27 → ', %23 → #, %2D%2D → --)
            String decoded = java.net.URLDecoder.decode(input, StandardCharsets.UTF_8);
            // 如果解码后与原文不同，可能进行了编码绕过，进一步递归检测
            if (!decoded.equals(input)) {
                return normalize(decoded);
            }
            return decoded;
        } catch (Exception e) {
            return input;
        }
    }
}