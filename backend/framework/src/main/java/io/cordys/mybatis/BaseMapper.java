package io.cordys.mybatis;

import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.AbstractSQL;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * 通用 Mapper接口，提供基本的增删改查方法。
 *
 * @param <E> 实体类型
 */
public interface BaseMapper<E> {

    /**
     * 插入一条记录。
     *
     * @param entity 要插入的实体
     * @return 插入的行数
     */
    @InsertProvider(type = InsertSqlProvider.class, method = "invoke")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer insert(E entity);

    /**
     * 批量插入记录。
     *
     * @param entities 要插入的实体列表
     * @return 插入的行数
     */
    @InsertProvider(type = BatchInsertSqlProvider.class, method = "invoke")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer batchInsert(List<E> entities);

    /**
     * 根据主键更新记录。
     *
     * @param entity 要更新的实体
     * @return 更新的行数
     */
    @UpdateProvider(type = UpdateSqlProvider.class, method = "invoke")
    Integer updateById(E entity);

    /**
     * 选择性更新记录（仅更新非空字段）。
     *
     * @param entity 要更新的实体
     * @return 更新的行数
     */
    @UpdateProvider(type = UpdateSelectiveSqlProvider.class, method = "invoke")
    Integer update(E entity);

    /**
     * 根据主键删除记录。
     *
     * @param id 主键值
     * @return 删除的行数
     */
    @DeleteProvider(type = DeleteSqlProvider.class, method = "invoke")
    Integer deleteByPrimaryKey(Serializable id);

    /**
     * 根据条件删除记录。
     *
     * @param criteria 删除的条件
     * @return 删除的行数
     */
    @DeleteProvider(type = DeleteByCriteriaSqlProvider.class, method = "invoke")
    Integer delete(E criteria);

    /**
     * 使用 LambdaQueryWrapper 批量删除。
     *
     * @param wrapper LambdaQueryWrapper 删除条件
     */
    @DeleteProvider(type = DeleteByLambdaSqlProvider.class, method = "invoke")
    void deleteByLambda(@Param("wrapper") LambdaQueryWrapper<E> wrapper);

    /**
     * 批量删除。
     *
     * @param ids List 删除条件
     */
    @DeleteProvider(type = DeleteByIdsSqlProvider.class, method = "invoke")
    void deleteByIds(@Param("ids") List<String> ids);

    /**
     * 根据主键查询记录。
     *
     * @param id 主键值
     * @return 查询到的实体
     */
    @SelectProvider(type = SelectByIdSqlProvider.class, method = "invoke")
    E selectByPrimaryKey(Serializable id);

    /**
     * 查询所有记录。
     *
     * @param orderBy 排序条件
     * @return 查询到的实体列表
     */
    @SelectProvider(type = SelectAllSqlProvider.class, method = "invoke")
    List<E> selectAll(String orderBy);

    /**
     * 根据条件查询记录。
     *
     * @param criteria 查询条件
     * @return 查询到的实体列表
     */
    @SelectProvider(type = SelectByCriteriaSqlProvider.class, method = "invoke")
    List<E> select(E criteria);

    /**
     * 使用 LambdaQueryWrapper 查询记录列表。
     *
     * @param wrapper LambdaQueryWrapper 查询条件
     * @return 查询结果列表
     */
    @SelectProvider(type = SelectByLambdaSqlProvider.class, method = "invoke")
    List<E> selectListByLambda(@Param("wrapper") LambdaQueryWrapper<E> wrapper);

    /**
     * 根据条件查询单条记录。
     *
     * @param criteria 查询条件
     * @return 查询到的实体
     */
    @SelectProvider(type = SelectByCriteriaSqlProvider.class, method = "invoke")
    E selectOne(E criteria);

    /**
     * 根据列和值查询记录。
     *
     * @param column 列名
     * @param ids    查询的值数组
     * @return 查询到的实体列表
     */
    @SelectProvider(type = SelectInSqlProvider.class, method = "invoke")
    List<E> selectByColumn(@Param("column") String column, @Param("array") Serializable[] ids);

    /**
     * 根据条件统计记录数。
     *
     * @param criteria 查询条件
     * @return 记录数
     */
    @SelectProvider(type = CountByCriteriaSqlProvider.class, method = "invoke")
    Long countByExample(E criteria);

    /**
     * 根据主键数组查询记录。
     *
     * @param ids 主键值数组
     * @return 查询到的实体列表
     */
    default List<E> selectByIds(@Param("array") Serializable[] ids) {
        return selectByColumn("id", ids);
    }

    /**
     * 根据主键数组查询记录。
     *
     * @param ids 主键值数组
     * @return 查询到的实体列表
     */
    default List<E> selectByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return selectByIds(ids.toArray(new String[0]));
    }

    /**
     * 判断记录是否存在。
     *
     * @param criteria 查询条件
     * @return 如果存在返回true，否则返回false
     */
    default boolean exist(E criteria) {
        Long count = countByExample(criteria);
        return count != null && count > 0;
    }

    /**
     * 插入或更新记录。
     *
     * @param criteria 查询条件
     * @return 执行的行数
     */
    default Integer upsert(E criteria) {
        return exist(criteria) ? updateById(criteria) : insert(criteria);
    }

    class InsertSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object criteria) {
            SQL sql = new SQL()
                    .INSERT_INTO(table.getTableName())
                    .INTO_COLUMNS(table.getColumns())
                    .INTO_VALUES(Stream.of(table.getFields()).map(this::bindParameter).toArray(String[]::new));

            return String.format("<script>%s</script>", sql.toString());
        }
    }

    @SuppressWarnings("all")
    class BatchInsertSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object criteria) {
            SQL sql = new SQL()
                    .INSERT_INTO(table.getTableName())
                    .INTO_COLUMNS(table.getColumns())
                    .INTO_VALUES(Stream.of(table.getFields()).map(this::bindParameter).toArray(String[]::new));

            return String.format("<script>%s</script>", sql.toString());
        }
    }

    @SuppressWarnings("all")
    class UpdateSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object criteria) {
            return updateSQL();
        }
    }

    @SuppressWarnings("all")
    class UpdateSelectiveSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object entity) {
            return updateSQL();
        }
    }

    class DeleteSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object criteria) {
            SQL sql = new SQL()
                    .DELETE_FROM(table.getTableName())
                    .WHERE(table.getPrimaryKeyColumn() + " = #{id}");

            return String.format("<script>%s</script>", sql.toString());
        }
    }

    class DeleteByCriteriaSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object criteria) {
            StringBuilder sql = new StringBuilder();
            sql.append("<script>");
            sql.append("DELETE ");
            tableWhere(sql);
            sql.append("</script>");
            return sql.toString();
        }
    }

    // TODO 待优化
    class DeleteByLambdaSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object criteria) {
            LambdaQueryWrapper<?> wrapper = (LambdaQueryWrapper<?>) criteria;

            BaseMapper.SQL sql = new SQL().DELETE_FROM(table.getTableName());

            // 将 LambdaQueryWrapper 的条件解析为 WHERE 子句
            Optional.ofNullable(wrapper.getWhereClause())
                    .filter(StringUtils::isNotBlank)
                    .ifPresent(sql::WHERE);

            return String.format("<script>%s</script>", sql);
        }
    }

    @SuppressWarnings("all")
    class DeleteByIdsSqlProvider extends AbstractSqlProviderSupport implements WriteType {
        @Override
        public String sql(Object entities) {
            List<String> ids = (List<String>) entities;
            if (CollectionUtils.isEmpty(ids)) {
                throw new IllegalArgumentException("ids cannot be empty");
            }

            StringBuilder sql = new StringBuilder();
            sql.append("<script>");
            sql.append("DELETE FROM ").append(table.getTableName());
            sql.append("""
                        WHERE id IN
                         <foreach collection="array" item="item" open="(" separator="," close=")">
                          #{item}
                        </foreach>
                    """);
            sql.append("</script>");

            return sql.toString();
        }
    }

    class SelectByIdSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public String sql(Object criteria) {
            SQL sql = new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName())
                    .WHERE(table.getPrimaryKeyColumn() + " = #{id}");

            return String.format("<script>%s</script>", sql.toString());
        }
    }

    class SelectAllSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public String sql(Object criteria) {
            String orderBy = (String) criteria;
            BaseMapper.SQL sql = new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName());
            if (StringUtils.isBlank(orderBy)) {
                orderBy = table.getPrimaryKeyColumn() + " DESC";
            }
            sql.ORDER_BY(orderBy);

            return String.format("<script>%s</script>", sql);
        }
    }

    class SelectInSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public String sql(Object entities) {
            var param = (Map<?, ?>) entities;
            String inField = (String) param.get("column");
            Serializable[] ids = (Serializable[]) param.get("array");

            if (ids == null || ids.length == 0) {
                throw new IllegalArgumentException("array parameter cannot be empty");
            }

            return """
                    <script>
                        SELECT %s
                        FROM %s
                        WHERE %s IN
                        <foreach collection="array" item="item" open="(" separator="," close=")">
                            #{item}
                        </foreach>
                    </script>
                    """.formatted(String.join(", ", table.getSelectColumns()), table.getTableName(), inField);
        }
    }

    class SelectByCriteriaSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public String sql(Object criteria) {
            StringBuilder sql = new StringBuilder();
            sql.append("<script>");
            sql.append("SELECT ").append(String.join(", ", table.getSelectColumns()));
            tableWhere(sql);
            // 添加排序
            sql.append(" ORDER BY ").append(table.getPrimaryKeyColumn()).append(" DESC");

            sql.append("</script>");
            return sql.toString();
        }
    }

    // TODO 待优化
    class SelectByLambdaSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public String sql(Object criteria) {
            LambdaQueryWrapper<?> wrapper = (LambdaQueryWrapper<?>) criteria;

            BaseMapper.SQL sql = new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName());

            // 将 LambdaQueryWrapper 的条件解析为 WHERE 子句
            String whereClause = wrapper.getWhereClause();
            if (StringUtils.isNotBlank(whereClause)) {
                sql.WHERE(whereClause);
            }

            // 解析排序条件
            String orderByClause = wrapper.getOrderByClause();
            if (StringUtils.isNotBlank(orderByClause)) {
                sql.ORDER_BY(orderByClause);
            }
            return String.format("<script>%s</script>", sql.toString());
        }
    }

    class CountByCriteriaSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public String sql(Object criteria) {
            StringBuilder sql = new StringBuilder();
            sql.append("<script>");
            sql.append("SELECT COUNT(*)");
            tableWhere(sql);
            sql.append("</script>");

            return sql.toString();
        }
    }

    interface Interceptor {
        /**
         * 类似 @PrePersist
         **/
        default void prePersist() {
        }
    }

    interface WriteType {
    }

    class SQL extends AbstractSQL<SQL> {
        public SQL WHERE(boolean test, String condition) {
            return test ? super.WHERE(condition) : this;
        }

        @Override
        public SQL getSelf() {
            return this;
        }
    }
}