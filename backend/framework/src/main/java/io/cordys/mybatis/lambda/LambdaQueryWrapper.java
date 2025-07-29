package io.cordys.mybatis.lambda;

import io.cordys.common.uid.IDGenerator;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LambdaQueryWrapper 用于构建 SQL 查询条件，支持链式调用。
 * 它通过 Lambda 表达式动态指定查询字段，避免硬编码字段名，增强代码的可维护性和类型安全。
 *
 * @param <T> 实体类型
 */
public class LambdaQueryWrapper<T> {
    // 存储查询条件
    private final List<String> conditions = new ArrayList<>();

    @Getter
    private final Map<String, Object> params = new HashMap<>();

    // 存储排序条件
    private final List<String> orderByClauses = new ArrayList<>();

    private void addCondition(XFunction<T, ?> column, Object value, String op) {
        String columnName = columnToString(column);
        addCondition(columnName + " " + op + " #{" + columnName + "}");
        // 将参数添加到 params 中，便于后续使用
        params.put(columnName, formatValue(value));

    }

    /**
     * 添加等值条件（=）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> eq(XFunction<T, ?> column, Object value) {
        addCondition(column, value, "=");
        return this;
    }

    /**
     * 添加不等值条件（!=）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     */
    public LambdaQueryWrapper<T> nq(XFunction<T, ?> column, Object value) {
        addCondition(column, value, "!=");
        return this;
    }

    /**
     * 添加模糊匹配条件（LIKE）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> like(XFunction<T, ?> column, Object value) {
        addCondition(column, "%" + value + "%", "LIKE");
        return this;
    }

    /**
     * 添加大于条件（>）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> gt(XFunction<T, ?> column, Object value) {
        addCondition(column, value, ">");
        return this;
    }

    /**
     * 时间类型的添加大于条件（>）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> gtT(XFunction<T, ?> column, Object value) {
        addCondition(column, value, " <![CDATA[ > ]]> ");
        return this;
    }

    /**
     * 添加小于条件（<）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> lt(XFunction<T, ?> column, Object value) {
        addCondition(column, value, "<");
        return this;
    }

    /**
     * 时间类型的添加小于条件（<）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> ltT(XFunction<T, ?> column, Object value) {
        addCondition(column, value, " <![CDATA[ < ]]> ");
        return this;
    }

    /**
     * 添加范围条件（BETWEEN）。
     *
     * @param column 列名的 Lambda 表达式
     * @param start  起始值
     * @param end    结束值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> between(XFunction<T, ?> column, Object start, Object end) {
        String columnName = columnToString(column);
        String paramStartKey = IDGenerator.nextStr();
        String paramEndKey = IDGenerator.nextStr();

        String condition = String.format("%s BETWEEN #{%s} AND #{%s}", columnName, paramStartKey, paramEndKey);
        addCondition(condition);

        params.put(paramStartKey, formatValue(start));
        params.put(paramEndKey, formatValue(end));

        return this;
    }

    /**
     * 添加 IN 条件。
     *
     * @param column    列名的 Lambda 表达式
     * @param valueList 值的集合
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> in(XFunction<T, ?> column, List<?> valueList) {
        if (valueList == null || valueList.isEmpty()) {
            return this;
        }

        String columnName = columnToString(column);
        StringBuilder conditionSql = new StringBuilder(columnName).append(" IN (");

        for (int i = 0; i < valueList.size(); i++) {
            String paramKey = String.format("%s_in_%d", columnName, i);
            if (i > 0) {
                conditionSql.append(", ");
            }
            conditionSql.append("#{").append(paramKey).append("}");
            params.put(paramKey, formatValue(valueList.get(i)));
        }

        conditionSql.append(")");
        addCondition(conditionSql.toString());
        return this;
    }

    /**
     * 返回 true 表示存在 SQL 注入风险
     *
     * @param script
     */
    public static void checkSqlInjection(String script) {
        if (StringUtils.isEmpty(script)) {
            return;
        }
        // 检测危险SQL模式
        java.util.regex.Pattern dangerousPattern = java.util.regex.Pattern.compile(
                "(;|--|#|'|\"|/\\*|\\*/|\\b(select|insert|update|delete|drop|alter|truncate|exec|union|xp_)\\b)",
                java.util.regex.Pattern.CASE_INSENSITIVE);

        // 返回true表示存在注入风险
        if (dangerousPattern.matcher(script).find()) {
            throw new IllegalArgumentException("SQL injection risk detected in script: " + script);
        }
    }

    /**
     * 添加升序排序。
     *
     * @param column 列名的 Lambda 表达式
     */
    public LambdaQueryWrapper<T> orderByAsc(XFunction<T, ?> column) {
        String columnName = columnToString(column);
        checkSqlInjection(columnName);
        String paramKey = String.format("order_%s", columnName);

        orderByClauses.add(String.format("#{%s} ASC", paramKey));
        params.put(paramKey, columnName);
        return this;
    }

    /**
     * 添加降序排序。
     *
     * @param column 列名的 Lambda 表达式
     */
    public LambdaQueryWrapper<T> orderByDesc(XFunction<T, ?> column) {
        String columnName = columnToString(column);
        checkSqlInjection(columnName);
        String paramKey = String.format("order_%s", columnName);

        orderByClauses.add(String.format("#{%s} DESC", paramKey));
        params.put(paramKey, columnName);
        return this;
    }

    /**
     * 获取 WHERE 子句的字符串。
     *
     * @return WHERE 子句的字符串
     */
    public String getWhereClause() {
        return String.join(" AND ", conditions);
    }

    /**
     * 获取 ORDER BY 子句的字符串。
     *
     * @return ORDER BY 子句的字符串
     */
    public String getOrderByClause() {
        return orderByClauses.isEmpty() ? "" : String.join(", ", orderByClauses);
    }

    /**
     * 获取最终的 SQL 查询字符串，包括 WHERE 和 ORDER BY 子句。
     *
     * @return 完整的 SQL 查询字符串
     */
    public String getSql() {
        String where = getWhereClause();
        String orderBy = getOrderByClause();
        return (where.isEmpty() ? "" : "WHERE " + where) +
                (orderBy.isEmpty() ? "" : " " + orderBy);
    }

    /**
     * 内部方法：添加条件到查询条件列表。
     *
     * @param condition 条件字符串
     */
    private void addCondition(String condition) {
        if (condition != null && !condition.trim().isEmpty()) {
            conditions.add(condition);
        }
    }

    /**
     * 内部方法：将 Lambda 表达式转换为字段名。
     *
     * @param column 列名的 Lambda 表达式
     * @return 转换后的字段名
     */
    private String columnToString(XFunction<T, ?> column) {
        return LambdaUtils.extract(column);
    }

    /**
     * 内部方法：格式化值，以便在 SQL 查询中正确使用。
     *
     * @param value 要格式化的值
     * @return 格式化后的值
     */
    private Object formatValue(Object value) {
        return value;
    }
}
