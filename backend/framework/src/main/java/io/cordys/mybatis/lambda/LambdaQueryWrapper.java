package io.cordys.mybatis.lambda;

import java.util.ArrayList;
import java.util.List;

/**
 * LambdaQueryWrapper 用于构建 SQL 查询条件，支持链式调用。
 * 它通过 Lambda 表达式动态指定查询字段，避免硬编码字段名，增强代码的可维护性和类型安全。
 *
 * @param <T> 实体类型
 */
public class LambdaQueryWrapper<T> {
    // 存储查询条件
    private final List<String> conditions = new ArrayList<>();

    // 存储排序条件
    private final List<String> orderByClauses = new ArrayList<>();

    /**
     * 添加等值条件（=）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> eq(XFunction<T, ?> column, Object value) {
        addCondition(columnToString(column) + " = " + formatValue(value));
        return this;
    }

    /**
     * 添加不等值条件（!=）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value  值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> nq(XFunction<T, ?> column, Object value) {
        addCondition(columnToString(column) + " != " + formatValue(value));
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
        addCondition(columnToString(column) + " LIKE " + formatValue("%" + value + "%"));
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
        addCondition(columnToString(column) + " > " + formatValue(value));
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
        addCondition(columnToString(column) + " < " + formatValue(value));
        return this;
    }

    /**
     * 添加范围条件（BETWEEN）。
     *
     * @param column 列名的 Lambda 表达式
     * @param value1 起始值
     * @param value2 结束值
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> between(XFunction<T, ?> column, Object value1, Object value2) {
        addCondition(columnToString(column) + " BETWEEN " + formatValue(value1) + " AND " + formatValue(value2));
        return this;
    }

    /**
     * 添加 IN 条件。
     *
     * @param column 列名的 Lambda 表达式
     * @param values 值的集合
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> in(XFunction<T, ?> column, List<?> values) {
        String inValues = String.join(", ", values.stream().map(this::formatValue).toArray(String[]::new));
        addCondition(columnToString(column) + " IN (" + inValues + ")");
        return this;
    }

    /**
     * 添加升序排序。
     *
     * @param column 列名的 Lambda 表达式
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> orderByAsc(XFunction<T, ?> column) {
        orderByClauses.add(columnToString(column) + " ASC");
        return this;
    }

    /**
     * 添加降序排序。
     *
     * @param column 列名的 Lambda 表达式
     * @return 当前 LambdaQueryWrapper 实例
     */
    public LambdaQueryWrapper<T> orderByDesc(XFunction<T, ?> column) {
        orderByClauses.add(columnToString(column) + " DESC");
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
    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }
}
