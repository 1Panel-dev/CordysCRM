package cn.cordys.mybatis;

import lombok.Getter;

import java.lang.reflect.Field;

/**
 * 实体类与数据库表信息的映射类。
 * <p>
 * 此类用于描述数据库表与实体类的映射关系，包括表名、字段、主键等元信息。
 * 主要用于持久化层的数据映射与操作。
 * </p>
 */
public class EntityTable {

    /**
     * 表对应的实体类型。
     */
    @Getter
    private Class<?> entityClass;

    /**
     * 实体类型中不包含 {@code @NoColumn} 注解的字段。
     */
    private Field[] fields;

    /**
     * 数据库表名。
     */
    private String tableName;

    /**
     * 主键列名。
     */
    private String primaryKeyColumn;

    /**
     * 数据库表的所有列名。
     */
    private String[] columns;

    /**
     * 用于生成 SELECT SQL 的列名集合。
     * <p>
     * 如果字段名称中含有下划线，会转换为形如 "aa_bb AS aaBb" 的形式。
     * </p>
     */
    private String[] selectColumns;

    /**
     * 设置实体类型。
     *
     * @param entityClass 实体类的 Class 对象
     */
    void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * 获取不含 {@code @NoColumn} 注解的字段。
     *
     * @return 字段数组
     */
    Field[] getFields() {
        return fields;
    }

    /**
     * 设置字段数组。
     *
     * @param fields 字段数组
     */
    void setFields(Field[] fields) {
        this.fields = fields;
    }

    /**
     * 获取数据库表名。
     *
     * @return 表名
     */
    String getTableName() {
        return tableName;
    }

    /**
     * 设置数据库表名。
     *
     * @param tableName 表名
     */
    void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * 获取主键列名。
     *
     * @return 主键列名
     */
    String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    /**
     * 设置主键列名。
     *
     * @param primaryKeyColumn 主键列名
     */
    void setPrimaryKeyColumn(String primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }

    /**
     * 获取数据库表的所有列名。
     *
     * @return 列名数组
     */
    String[] getColumns() {
        return columns;
    }

    /**
     * 设置数据库表的所有列名。
     *
     * @param columns 列名数组
     */
    void setColumns(String[] columns) {
        this.columns = columns;
    }

    /**
     * 获取用于生成 SELECT SQL 的列名集合。
     *
     * @return 列名数组
     */
    String[] getSelectColumns() {
        return selectColumns;
    }

    /**
     * 设置用于生成 SELECT SQL 的列名集合。
     *
     * @param selectColumns 列名数组
     */
    void setSelectColumns(String[] selectColumns) {
        this.selectColumns = selectColumns;
    }
}
