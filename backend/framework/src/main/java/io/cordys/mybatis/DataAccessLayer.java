package io.cordys.mybatis;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.CodingUtils;
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


    // 添加LRU缓存实现
    private final Map<Class<?>, EntityTable> cachedTableInfo = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Class<?>, EntityTable> eldest) {
                    return size() > 1000;
                }
            });

    // 添加SQL语句缓存，避免重复创建MappedStatement
    private final Map<String, String> sqlCache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > 5000;
                }
            });

    private DataAccessLayer() {
    }

    /**
     * 初始化 SqlSession，用于执行 DAL 操作。
     *
     * @param sqlSession SqlSession，用于数据库交互。
     */
    private void initSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
        this.configuration = sqlSession.getConfiguration();
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext context) throws BeansException {
        synchronized (DataAccessLayer.class) {
            if (applicationContext == null) {
                applicationContext = context;
            }
        }
    }

    /**
     * 单例持有者，确保 Dal 实例的唯一性。
     */
    private static class Holder {
        private static final DataAccessLayer instance = new DataAccessLayer();
    }

    /**
     * 获取 Dal 实例并为指定的实体类准备 Executor。
     *
     * @param clazz 实体类类型。
     * @param <T>   实体类的类型。
     * @return 为指定实体类准备的 Executor。
     */
    public static <T> Executor<T> with(Class<T> clazz) {
        return with(clazz, applicationContext.getBean(SqlSession.class));
    }

    /**
     * 获取 Dal 实例并为指定的实体类准备 Executor，同时使用给定的 SqlSession。
     *
     * @param clazz      实体类类型。
     * @param sqlSession SqlSession，用于数据库交互。
     * @param <T>        实体类的类型。
     * @return 为指定实体类准备的 Executor。
     */
    public static <T> Executor<T> with(Class<T> clazz, SqlSession sqlSession) {
        DataAccessLayer instance = Holder.instance;
        instance.initSession(sqlSession);
        EntityTable entityTable = null;
        if (clazz != null) {
            entityTable = instance.cachedTableInfo.computeIfAbsent(clazz, EntityTableMapper::extractTableInfo);
        }
        return instance.new Executor<>(entityTable);
    }

    /**
     * 执行原生 SQL 查询，并将结果作为对象列表返回。
     *
     * @param sql        要执行的 SQL 查询语句。
     * @param param      查询参数。
     * @param resultType 结果类型。
     * @param <T>        结果类型的泛型。
     * @return 查询结果的对象列表。
     */
    public static <T> List<T> sql(String sql, Object param, Class<T> resultType) {
        return with(resultType).sqlQuery(sql, param, resultType);
    }

    public class Executor<E> implements BaseMapper<E> {
        private final EntityTable table;
        private final Class<?> resultType;

        Executor(EntityTable table) {
            this.table = table;
            this.resultType = table != null ? table.getEntityClass() : null;
        }

        @Override
        public List<E> query(Function<SQL, SQL> sqlBuild, Object criteria) {
            Map<String, Object> maps = new HashMap<>(2);
            maps.put("sqlBuild", sqlBuild);
            maps.put("entity", criteria);
            String sql = new BaseMapper.SelectBySqlProvider().buildSql(maps, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> selectAll(String criteria) {
            String sql = new BaseMapper.SelectAllSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> select(E criteria) {
            String sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public List<E> selectListByLambda(LambdaQueryWrapper<E> wrapper) {
            String sql = new BaseMapper.SelectByLambdaSqlProvider().buildSql(wrapper, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, wrapper);
        }

        @Override
        public E selectByPrimaryKey(Serializable criteria) {
            String sql = new BaseMapper.SelectByIdSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public E selectOne(E criteria) {
            String sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public List<E> selectByColumn(String column, Serializable[] criteria) {
            Map<String, Object> maps = new HashMap<>(2);
            maps.put("column", column);
            maps.put("array", criteria);
            String sql = new BaseMapper.SelectInSqlProvider().buildSql(maps, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }

        @Override
        public Long countByExample(E criteria) {
            String sql = new BaseMapper.CountByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), Long.class, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }

        @Override
        public Integer insert(E criteria) {
            String sql = new BaseMapper.InsertSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.INSERT);
            return sqlSession.insert(msId, criteria);
        }

        @Override
        public Integer updateById(E criteria) {
            String sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }

        @Override
        public Integer update(E criteria) {
            String sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }

        @Override
        public Integer delete(E criteria) {
            String sql = new BaseMapper.DeleteByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }

        @Override
        public void deleteByLambda(LambdaQueryWrapper<E> wrapper) {
            String sql = new BaseMapper.DeleteByLambdaSqlProvider().buildSql(wrapper, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
            sqlSession.delete(msId, wrapper);
        }

        @Override
        public void deleteByIds(List<String> array) {
            String sql = new BaseMapper.DeleteByIdsSqlProvider().buildSql(array, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
            Map<String, List<String>> map = new HashMap<>();
            map.put("array", array);
            sqlSession.delete(msId, map);
        }

        @Override
        public Integer deleteByPrimaryKey(Serializable criteria) {
            String sql = new BaseMapper.DeleteSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }

        private void checkDangerousSql(String sql) {
            if (sql == null) {
                return;
            }
            if (sql.toLowerCase().contains("drop") || sql.toLowerCase().contains("truncate")) {
                throw new IllegalArgumentException("不允许执行危险SQL操作");
            }
        }

        public List<E> sqlQuery(String sql, Object param, Class<?> resultType) {
            checkDangerousSql(sql);
            return sqlQuery(sql, param, param != null ? param.getClass() : Map.class, resultType);
        }

        public List<E> sqlQuery(String sql, Object param, Class<?> paramType, Class<?> resultType) {
            checkDangerousSql(sql);
            String msId = execute(sql, paramType, resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, param);
        }

        @Override
        public boolean exist(E criteria) {
            List<E> ret = select(criteria);
            return ret != null && !ret.isEmpty();
        }

        @Override
        public Integer batchInsert(List<E> entities) {
            if (entities == null || entities.isEmpty()) {
                return 0;
            }

            // 批量处理，避免一次性处理过多数据
            int batchSize = 500; // 每批处理500条数据
            int totalSize = entities.size();
            int totalBatches = (totalSize + batchSize - 1) / batchSize;
            int insertedCount = 0;

            SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);

            for (int i = 0; i < totalBatches; i++) {
                int start = i * batchSize;
                int end = Math.min((i + 1) * batchSize, totalSize);
                List<E> batchEntities = entities.subList(start, end);

                // 构建SQL
                String sql = new BaseMapper.BatchInsertSqlProvider().buildSql(batchEntities, this.table);
                String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.INSERT);

                // 使用try-with-resources确保SqlSession关闭
                try (SqlSession batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
                    for (E entity : batchEntities) {
                        batchSession.insert(msId, entity);
                    }
                    batchSession.flushStatements();
                    batchSession.commit();
                    insertedCount += (end - start);
                } catch (Exception e) {
                    throw new RuntimeException("批量插入失败", e);
                }
            }

            return insertedCount;
        }
    }

    /**
     * 生成更合理的缓存键
     */
    private String generateCacheKey(String sql, Class<?> parameterType, Class<?> resultType, SqlCommandType sqlCommandType) {
        // 1. SQL规范化处理
        String normalizedSql = normalizeSql(sql);

        // 2. 使用StringBuilder提高效率
        StringBuilder keyBuilder = new StringBuilder(64)
                .append(sqlCommandType.name())
                .append(':')
                .append(parameterType.getName())
                .append(':')
                .append(resultType.getName())
                .append(':');

        // 3. 使用哈希码代替完整SQL
        keyBuilder.append(CodingUtils.hashStr(normalizedSql));

        return keyBuilder.toString();
    }

    /**
     * SQL规范化处理
     */
    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        // 去除多余空格、统一换行符、转为小写
        return sql.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    /**
     * 执行 SQL，并返回生成的 MappedStatement ID。
     * 使用SQL语句缓存，避免重复创建MappedStatement
     *
     * @param sql            SQL 查询语句。
     * @param parameterType  参数类型。
     * @param resultType     结果类型。
     * @param sqlCommandType SQL 命令类型。
     * @return MappedStatement 的 ID。
     */
    private String execute(String sql, Class<?> parameterType, Class<?> resultType, SqlCommandType sqlCommandType) {
        // 生成缓存键
        String cacheKey = generateCacheKey(sql, parameterType, resultType, sqlCommandType);

        // 尝试从缓存获取
        String msId = sqlCache.get(cacheKey);
        if (msId != null && configuration.hasStatement(msId, false)) {
            return msId;
        }

        // 没有缓存，创建新的MappedStatement
        msId = cacheKey + "." + IDGenerator.nextStr();

        SqlSource sqlSource = configuration
                .getDefaultScriptingLanguageInstance()
                .createSqlSource(configuration, sql, parameterType);

        // 创建并缓存MappedStatement
        newMappedStatement(msId, sqlSource, resultType, sqlCommandType);

        // 缓存SQL和msId的映射
        sqlCache.put(cacheKey, msId);

        return msId;
    }

    /**
     * 创建并注册新的 MappedStatement。
     *
     * @param msId           MappedStatement 的 ID。
     * @param sqlSource      SQL 源。
     * @param resultType     结果类型。
     * @param sqlCommandType SQL 命令类型。
     */
    private void newMappedStatement(String msId, SqlSource sqlSource, Class<?> resultType, SqlCommandType sqlCommandType) {
        MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, sqlCommandType)
                .resultMaps(Collections.singletonList(
                        new ResultMap.Builder(configuration, "defaultResultMap", resultType, new ArrayList<>(0)).build()
                ))
                .build();
        configuration.addMappedStatement(ms);
    }
}