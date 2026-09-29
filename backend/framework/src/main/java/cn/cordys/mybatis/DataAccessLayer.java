package cn.cordys.mybatis;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据访问层（DAL），提供与数据库交互的方法，使用 MyBatis 进行 SQL 查询的执行。
 * 支持多种 CRUD 操作。
 */
@Component
@Slf4j
public class DataAccessLayer implements ApplicationContextAware {

    private static volatile ApplicationContext applicationContext;
    // 使用 ConcurrentHashMap 替代同步 LinkedHashMap，提高并发性能
    private final Map<Class<?>, EntityTable> cachedTableInfo = new ConcurrentHashMap<>(128);
    // 添加 MappedStatement 缓存计数监控
    private final AtomicInteger mappedStatementCount = new AtomicInteger(0);

    private DataAccessLayer() {
        // 私有构造函数
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
        var instance = Holder.INSTANCE;

        var entityTable = Optional.ofNullable(entityClass)
                .map(clazz -> instance.cachedTableInfo.computeIfAbsent(
                        clazz, EntityTableMapper::extractTableInfo))
                .orElse(null);

        return instance.new Executor<>(entityTable, sqlSession);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
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
     * 生成缓存键
     */
    private String generateCacheKey(String sql, String methodName, Class<?> parameterType, SqlCommandType sqlCommandType) {
        // 排序、查询列等会改变 SQL，不能只按实体和方法复用第一次生成的语句。
        return String.format("%s:%s:%s:%s", sqlCommandType.name(), parameterType.getName(), methodName, CodingUtils.hashStr(sql));
    }

    /**
     * 执行 SQL，返回 MappedStatement ID
     */
    private String execute(Configuration configuration, String sql, String methodName, Class<?> parameterType, Class<?> resultType, SqlCommandType sqlCommandType) {
        var msId = generateCacheKey(sql, methodName, parameterType, sqlCommandType);

        if (!configuration.hasStatement(msId, false)) {
            // ponytail: 同一 Configuration 串行注册；首次注册成为瓶颈时再按语句保留锁。
            synchronized (configuration) {
                if (!configuration.hasStatement(msId, false)) {
                    // 创建和注册 MappedStatement
                    var sqlSource = configuration
                            .getDefaultScriptingLanguageInstance()
                            .createSqlSource(configuration, sql, parameterType);

                    newMappedStatement(configuration, msId, sqlSource, resultType, sqlCommandType);

                    var count = mappedStatementCount.incrementAndGet();
                    if (count % 500 == 0) {
                        log.info("当前缓存的 MappedStatement 总量：{}", count);
                    }
                }
            }
        }

        return msId;
    }

    /**
     * 创建并注册新的 MappedStatement
     */
    private void newMappedStatement(Configuration configuration, String msId, SqlSource sqlSource, Class<?> resultType, SqlCommandType sqlCommandType) {
        var resultMap = new ResultMap.Builder(configuration, "defaultResultMap", resultType, new ArrayList<>(0))
                .build();

        var ms = new MappedStatement.Builder(configuration, msId, sqlSource, sqlCommandType)
                .resultMaps(Collections.singletonList(resultMap))
                .build();

        configuration.addMappedStatement(ms);
    }

    /**
     * 单例持有者，确保 Dal 实例的唯一性
     */
    private static class Holder {
        private static final DataAccessLayer INSTANCE = new DataAccessLayer();
    }

    /**
     * 数据访问执行器
     */
    public class Executor<E> implements BaseMapper<E> {
        private final EntityTable table;
        private final Class<?> resultType;
        private final SqlSession sqlSession;
        private final Configuration configuration;

        Executor(EntityTable table, SqlSession sqlSession) {
            this.table = table;
            this.resultType = table != null ? table.getEntityClass() : null;
            // 执行器绑定创建时的会话，后续 with() 调用不能切换已有执行器的数据源或事务。
            this.sqlSession = Objects.requireNonNull(sqlSession, "SqlSession 不能为空");
            this.configuration = sqlSession.getConfiguration();
        }

        @Override
        public List<E> selectAll(String orderBy) {
            var sql = new SelectAllSqlProvider().buildSql(orderBy, this.table);
            var msId = execute(configuration, sql, "BaseMapper.selectAll", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, orderBy);
        }

        @Override
        public List<E> select(E criteria) {
            var sql = new SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.select", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> selectListByLambda(LambdaQueryWrapper<E> wrapper) {
            var sql = new SelectByLambdaSqlProvider().buildSql(wrapper, this.table);
            var msId = execute(configuration, sql, "BaseMapper.selectListByLambda", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, wrapper.getParams());
        }

        @Override
        public E selectByPrimaryKey(Serializable criteria) {
            var sql = new SelectByIdSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.selectByPrimaryKey", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public E selectOne(E criteria) {
            var sql = new SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.selectOne", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public List<E> selectByColumn(String column, Serializable[] criteria) {
            var params = new HashMap<String, Object>();
            params.put("column", column);
            params.put("array", criteria);

            var sql = new SelectInSqlProvider().buildSql(params, this.table);
            var msId = execute(configuration, sql, "BaseMapper.selectByColumn", table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, params);
        }

        @Override
        public Long countByExample(E criteria) {
            var sql = new CountByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.countByExample", table.getEntityClass(), Long.class, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public Integer insert(E criteria) {
            var sql = new InsertSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.insert", table.getEntityClass(), int.class, SqlCommandType.INSERT);
            return sqlSession.insert(msId, criteria);
        }

        @Override
        public Integer updateById(E criteria) {
            var sql = new UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.updateById", table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }

        @Override
        public Integer update(E criteria) {
            var sql = new UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.update", table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }

        @Override
        public Integer delete(E criteria) {
            var sql = new DeleteByCriteriaSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.delete", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }

        @Override
        public void deleteByLambda(LambdaQueryWrapper<E> wrapper) {
            var sql = new DeleteByLambdaSqlProvider().buildSql(wrapper, this.table);
            var msId = execute(configuration, sql, "BaseMapper.deleteByLambda", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            sqlSession.delete(msId, wrapper.getParams());
        }

        @Override
        public void deleteByIds(List<String> ids) {
            var sql = new DeleteByIdsSqlProvider().buildSql(ids, this.table);
            var msId = execute(configuration, sql, "BaseMapper.deleteByIds", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            var params = new HashMap<>();
            params.put("array", ids);
            sqlSession.delete(msId, params);
        }

        @Override
        public Integer deleteByPrimaryKey(Serializable criteria) {
            var sql = new DeleteSqlProvider().buildSql(criteria, this.table);
            var msId = execute(configuration, sql, "BaseMapper.deleteByPrimaryKey", table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
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

            if (!(sqlSession instanceof SqlSessionTemplate template)) {
                // ponytail: 原始会话逐条调用 insert；需要 JDBC 批处理时传入 BATCH 会话，提交仍由调用方控制。
                entities.forEach(this::insert);
                return entities.size();
            }

            var sql = new BatchInsertSqlProvider().buildSql(entities, this.table);
            var msId = execute(configuration, sql, "BaseMapper.batchInsert", table.getEntityClass(), int.class, SqlCommandType.INSERT);
            var sqlSessionFactory = template.getSqlSessionFactory();

            try (var batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
                entities.forEach(entity -> batchSession.insert(msId, entity));
                batchSession.flushStatements();
                batchSession.commit();
                // 批量会话只清理自己的一级缓存；当前事务中的查询会话也需要失效。
                sqlSession.clearCache();
                return entities.size();
            } catch (Exception e) {
                throw new RuntimeException("批量插入失败", e);
            }
        }
    }
}
