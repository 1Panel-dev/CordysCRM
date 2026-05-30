package cn.cordys.mybatis;

import cn.cordys.common.util.CaseFormatUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 工具类：EntityTableMapper
 * <p>
 * 提供基于反射的字段操作，包括字段过滤、表信息提取等功能。
 * </p>
 */
public class EntityTableMapper {

    private static final String FIELD_SEP_TAG = "`";

    /**
     * 获取实体类的表信息。
     *
     * @param entityClass 实体类
     *
     * @return 表信息对象
     */
    public static EntityTable extractTableInfo(Class<?> entityClass) {
        Predicate<Field> fieldFilter = field -> !"serialVersionUID".equals(field.getName());

        Field[] fields = filterFieldsWithoutNoColumnAnnotation(getAllFields(entityClass, fieldFilter));

        EntityTable info = new EntityTable();
        info.setEntityClass(entityClass);
        info.setFields(fields);
        info.setTableName(generateTableName(entityClass));
        info.setPrimaryKeyColumn(determinePrimaryKey(fields, "id"));
        info.setColumns(mapFieldsToColumnNames(fields));
        info.setSelectColumns(mapFieldsToSelectColumnNames(fields));
        return info;
    }

    /**
     * 获取字段对应的值。
     *
     * @param bean  对象实例
     * @param field 字段
     *
     * @return 字段值
     */
    public static synchronized Object getFieldValue(Object bean, Field field) {
        if (bean == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(bean);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法访问字段：" + field.getName(), e);
        } finally {
            field.setAccessible(false);
        }
    }

    /**
     * 确定主键字段对应的数据库列名。
     *
     * @param fields            实体类的字段数组
     * @param defaultPrimaryKey 默认主键名
     *
     * @return 主键的列名
     */
    public static String determinePrimaryKey(Field[] fields, String defaultPrimaryKey) {
        return Stream.of(fields)
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .map(EntityTableMapper::getColumnName)
                .orElse(defaultPrimaryKey);
    }

    /**
     * 生成表名（默认采用驼峰转下划线规则）。
     *
     * @param entityType 实体类
     *
     * @return 表名
     */
    public static String generateTableName(Class<?> entityType) {
        // 尝试获取 @Table 注解
        return Optional.ofNullable(entityType.getAnnotation(Table.class))
                .map(Table::name) // 如果有注解，获取 name 属性值
                .filter(name -> !name.isEmpty()) // 确保 name 属性非空
                .orElseGet(() -> CaseFormatUtils.camelToUnderscore(entityType.getSimpleName())); // 否则使用默认策略
    }

    /**
     * 将字段数组映射为数据库列名数组。
     *
     * @param fields 字段数组
     *
     * @return 列名数组
     */
    public static String[] mapFieldsToColumnNames(Field[] fields) {
        return Stream.of(fields)
                .map(EntityTableMapper::getColumnName)
                .toArray(String[]::new);
    }

    /**
     * 过滤掉带有 @NoColumn 注解的字段。
     *
     * @param fields 字段数组
     *
     * @return 过滤后的字段数组
     */
    public static Field[] filterFieldsWithoutNoColumnAnnotation(Field[] fields) {
        return Stream.of(fields)
                .filter(field -> !field.isAnnotationPresent(Transient.class))
                .toArray(Field[]::new);
    }

    /**
     * 将字段数组映射为查询列名数组（支持别名）。
     *
     * @param fields 字段数组
     *
     * @return 查询列名数组
     */
    public static String[] mapFieldsToSelectColumnNames(Field[] fields) {
        return Stream.of(fields)
                .map(EntityTableMapper::getSelectColumnName)
                .toArray(String[]::new);
    }

    /**
     * 获取字段的数据库列名（带下划线）。
     *
     * @param field 字段
     *
     * @return 数据库列名
     */
    public static String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        }
        return FIELD_SEP_TAG + CaseFormatUtils.camelToUnderscore(field.getName()) + FIELD_SEP_TAG;
    }

    /**
     * 获取字段的查询列名（带别名）。
     *
     * @param field 字段
     *
     * @return 查询列名
     */
    public static String getSelectColumnName(Field field) {
        return getColumnName(field) + " AS " + FIELD_SEP_TAG + field.getName() + FIELD_SEP_TAG;
    }


    /**
     * 获取指定类的所有字段（包括父类）。
     *
     * @param clazz       类
     * @param fieldFilter 字段过滤器
     *
     * @return 字段数组
     */
    public static Field[] getAllFields(Class<?> clazz, Predicate<Field> fieldFilter) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (fieldFilter == null || fieldFilter.test(field)) {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
}
