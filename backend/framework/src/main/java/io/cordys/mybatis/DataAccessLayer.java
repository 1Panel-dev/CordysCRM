package io.cordys.mybatis;

import io.cordys.common.util.LogUtils;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 数据访问层（DAL），提供与数据库交互的方法，使用 MyBatis 进行 SQL 查询的执行。
 * 支持多种 CRUD 操作。
 */
@Component
public class DataAccessLayer implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private SqlSession sqlSession;
    private Configuration configuration;

    // 使用 ConcurrentHashMap 替代同步 LinkedHashMap，提高并发性能
    private final Map<Class<?>, EntityTable> cachedTableInfo = new ConcurrentHashMap<>(128);

    // 添加 MappedStatement 缓存计数监控
    private final AtomicInteger mappedStatementCount = new AtomicInteger(0);

    // SQL 危险关键词集合
    private static final Set<String> DANGEROUS_SQL_KEYWORDS = Set.of("drop", "truncate");

    private DataAccessLayer() {
        // 私有构造函数
    }

    /**
     * 初始化 SqlSession，用于执行 DAL 操作
     */
    private DataAccessLayer initSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
        this.configuration = sqlSession.getConfiguration();
        return this;
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext context) throws BeansException {
        // 使用双重检查锁定模式
        if (applicationContext == null) {
            synchronized (DataAccessLayer.class) {
                if (applicationContext == null) {
                    applicationContext = context;
                }
            }
        }
    }

    /**
     * 单例持有者，确保 Dal 实例的唯一性
     */
    private static class Holder {
        private static final DataAccessLayer INSTANCE = new DataAccessLayer();
    }

    /**
     * 获取 Dal 实例并为指定的实体类准备 Executor
     */
    public static <T> Executor<T> with(Class<T> entityClass) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext 未初始化");
        }
        return with(entityClass, applicationContext.getBean(SqlSession.class));
    }

    /**
     * 获取 Dal 实例并使用指定的 SqlSession
     */
    public static <T> Executor<T> with(Class<T> entityClass, SqlSession sqlSession) {
        var instance = Holder.INSTANCE.initSession(sqlSession);

        var entityTable = Optional.ofNullable(entityClass)
                .map(clazz -> instance.cachedTableInfo.computeIfAbsent(
                        clazz, EntityTableMapper::extractTableInfo))
                .orElse(null);

        return instance.new Executor<>(entityTable);
    }

    /**
     * 执行原生 SQL 查询
     */
    public static <T> List<T> sql(String sql, Object param, Class<T> resultType) {
        return with(resultType).sqlQuery(sql, param, resultType);
    }

    /**
     * 数据访问执行器
     */
    public class Executor<E> implements BaseMapper<E> {
        private final EntityTable table;
        private final Class<?> resultType;

        Executor(EntityTable table) {
            this.table = table;
            this.resultType = table != null ? table.getEntityClass() : null;
        }

        @Override
        public List<E> query(Function<SQL, SQL> sqlBuild, Object criteria) {
            var params = Map.of(
                    "sqlBuild", sqlBuild,
                    "entity", criteria
            );
            var sql = new BaseMapper.SelectBySqlProvider().buildSql(params, this.table);
            var msId = execute(sql, "BaseMapper.query", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> selectAll(String criteria) {
            var sql = new BaseMapper.SelectAllSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.selectAll", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> select(E criteria) {
            var sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.select", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> selectListByLambda(LambdaQueryWrapper<E> wrapper) {
            var sql = new BaseMapper.SelectByLambdaSqlProvider().buildSql(wrapper, this.table);
            var msId = execute(sql, "BaseMapper.selectListByLambda", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, wrapper);
        }

        @Override
        public E selectByPrimaryKey(Serializable criteria) {
            var sql = new BaseMapper.SelectByIdSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.selectByPrimaryKey", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public E selectOne(E criteria) {
            var sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.selectOne", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public List<E> selectByColumn(String column, Serializable[] criteria) {
            var params = Map.of(
                    "column", column,
                    "array", criteria
            );
            var sql = new BaseMapper.SelectInSqlProvider().buildSql(params, this.table);
            var msId = execute(sql, "BaseMapper.selectByColumn", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public Long countByExample(E criteria) {
            var sql = new BaseMapper.CountByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.countByExample", table.getEntityClass(), Long.class, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public Integer insert(E criteria) {
            var sql = new BaseMapper.InsertSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.insert", table.getEntityClass(), int.class, SqlCommandType.INSERT);
            return sqlSession.insert(msId, criteria);
        }

        @Override
        public Integer updateById(E criteria) {
            var sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.updateById", table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }

        @Override
        public Integer update(E criteria) {
            var sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.update", table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }

        @Override
        public Integer delete(E criteria) {
            var sql = new BaseMapper.DeleteByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.delete", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }

        @Override
        public void deleteByLambda(LambdaQueryWrapper<E> wrapper) {
            var sql = new BaseMapper.DeleteByLambdaSqlProvider().buildSql(wrapper, this.table);
            var msId = execute(sql, "BaseMapper.deleteByLambda", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            sqlSession.delete(msId, wrapper);
        }

        @Override
        public void deleteByIds(List<String> ids) {
            var sql = new BaseMapper.DeleteByIdsSqlProvider().buildSql(ids, this.table);
            var msId = execute(sql, "BaseMapper.deleteByIds", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            var params = Map.of("array", ids);
            sqlSession.delete(msId, params);
        }

        @Override
        public Integer deleteByPrimaryKey(Serializable criteria) {
            var sql = new BaseMapper.DeleteSqlProvider().buildSql(criteria, this.table);
            var msId = execute(sql, "BaseMapper.deleteByPrimaryKey", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }

        /**
         * 检查SQL是否包含危险操作
         */
        private void checkDangerousSql(String sql) {
            if (sql == null) {
                return;
            }

            var lowerSql = sql.toLowerCase();
            boolean hasDangerousKeyword = DANGEROUS_SQL_KEYWORDS.stream()
                    .anyMatch(lowerSql::contains);

            if (hasDangerousKeyword) {
                throw new IllegalArgumentException("不允许执行危险SQL操作");
            }
        }

        public List<E> sqlQuery(String sql, Object param, Class<?> resultType) {
            checkDangerousSql(sql);
            var paramType = param != null ? param.getClass() : Map.class;
            return sqlQuery(sql, param, paramType, resultType);
        }

        public List<E> sqlQuery(String sql, Object param, Class<?> paramType, Class<?> resultType) {
            checkDangerousSql(sql);
            var msId = execute(sql, "BaseMapper.sqlQuery", paramType, resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, param);
        }

        @Override
        public boolean exist(E criteria) {
            return Optional.ofNullable(select(criteria))
                    .filter(list -> !list.isEmpty())
                    .isPresent();
        }

        @Override
        public Integer batchInsert(List<E> entities) {
            if (entities == null || entities.isEmpty()) {
                return 0;
            }

            var sql = new BaseMapper.BatchInsertSqlProvider().buildSql(entities, this.table);
            var msId = execute(sql, "BaseMapper.batchInsert", table.getEntityClass(), int.class, SqlCommandType.INSERT);
            var sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);

            try (var batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
                entities.forEach(entity -> batchSession.insert(msId, entity));
                batchSession.flushStatements();
                batchSession.commit();
                return entities.size();
            } catch (Exception e) {
                throw new RuntimeException("批量插入失败", e);
            }
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String methodName, Class<?> parameterType, SqlCommandType sqlCommandType) {
        return String.format("%s:%s:%s", sqlCommandType.name(), parameterType.getName(), methodName);
    }

    /**
     * 执行 SQL，返回 MappedStatement ID
     */
    private String execute(String sql, String methodName, Class<?> parameterType, Class<?> resultType, SqlCommandType sqlCommandType) {
        var msId = generateCacheKey(methodName, parameterType, sqlCommandType);

        if (!configuration.hasStatement(msId, false)) {
            var sqlSource = configuration
                    .getDefaultScriptingLanguageInstance()
                    .createSqlSource(configuration, sql, parameterType);

            newMappedStatement(msId, sqlSource, resultType, sqlCommandType);

            var count = mappedStatementCount.incrementAndGet();
            if (count % 500 == 0) {
                LogUtils.info("当前缓存的 MappedStatement 总量：{}", count);
            }
        }

        return msId;
    }

    /**
     * 创建并注册新的 MappedStatement
     */
    private void newMappedStatement(String msId, SqlSource sqlSource, Class<?> resultType, SqlCommandType sqlCommandType) {
        var resultMap = new ResultMap.Builder(configuration, "defaultResultMap", resultType, new ArrayList<>(0))
                .build();

        var ms = new MappedStatement.Builder(configuration, msId, sqlSource, sqlCommandType)
                .resultMaps(Collections.singletonList(resultMap))
                .build();

        configuration.addMappedStatement(ms);
    }
}