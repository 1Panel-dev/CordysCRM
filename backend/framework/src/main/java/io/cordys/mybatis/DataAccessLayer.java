package io.cordys.mybatis;

import io.cordys.common.uid.IDGenerator;
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
import java.util.function.Function;

/**
 * 数据访问层（DAL），提供与数据库交互的方法，使用 MyBatis 进行 SQL 查询的执行。
 * 支持多种 CRUD 操作。
 */
@Component
public class DataAccessLayer implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private final Map<Class<?>, EntityTable> cachedTableInfo = new ConcurrentHashMap<>();
    // 添加MappedStatement的缓存，限制大小
    private final Map<String, String> cachedMappedStatements = new LRUCache<>(1000);

    // 实现LRU缓存
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(maxSize, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    private DataAccessLayer() {
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 获取SqlSessionFactory
     */
    private SqlSessionFactory getSqlSessionFactory() {
        return applicationContext.getBean(SqlSessionFactory.class);
    }

    /**
     * 单例持有者，确保 Dal 实例的唯一性。
     */
    private static class Holder {
        private static final DataAccessLayer instance = new DataAccessLayer();
    }

    /**
     * 获取 Dal 实例并为指定的实体类准备 Executor。
     */
    public static <T> Executor<T> with(Class<T> clazz) {
        DataAccessLayer instance = Holder.instance;
        EntityTable entityTable = null;
        if (clazz != null) {
            entityTable = instance.cachedTableInfo.computeIfAbsent(clazz, EntityTableMapper::extractTableInfo);
        }
        return instance.new Executor<>(entityTable);
    }

    /**
     * 执行原生 SQL 查询，并将结果作为对象列表返回。
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
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                Map<String, Object> maps = new HashMap<>(2);
                maps.put("sqlBuild", sqlBuild);
                maps.put("entity", criteria);
                String sql = new BaseMapper.SelectBySqlProvider().buildSql(maps, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectList(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("查询执行失败", e);
            }
        }

        @Override
        public List<E> selectAll(String criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String sql = new BaseMapper.SelectAllSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectList(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("查询全部记录失败", e);
            }
        }

        @Override
        public List<E> select(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectList(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("条件查询失败", e);
            }
        }

        @Override
        public List<E> selectListByLambda(LambdaQueryWrapper<E> wrapper) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String sql = new BaseMapper.SelectByLambdaSqlProvider().buildSql(wrapper, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectList(msId, wrapper);
            } catch (Exception e) {
                throw new RuntimeException("Lambda查询失败", e);
            }
        }

        @Override
        public E selectByPrimaryKey(Serializable criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String sql = new BaseMapper.SelectByIdSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectOne(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("主键查询失败", e);
            }
        }

        @Override
        public E selectOne(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectOne(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("查询单条记录失败", e);
            }
        }

        @Override
        public List<E> selectByColumn(String column, Serializable[] criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                Map<String, Object> maps = new HashMap<>(2);
                maps.put("column", column);
                maps.put("array", criteria);
                String sql = new BaseMapper.SelectInSqlProvider().buildSql(maps, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
                return sqlSession.selectList(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("列查询失败", e);
            }
        }

        @Override
        public Long countByExample(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String sql = new BaseMapper.CountByCriteriaSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), Long.class, SqlCommandType.SELECT);
                return sqlSession.selectOne(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("计数查询失败", e);
            }
        }

        @Override
        public Integer insert(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.InsertSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.INSERT);
                return sqlSession.insert(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("插入操作失败", e);
            }
        }

        @Override
        public Integer updateById(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.UPDATE);
                return sqlSession.update(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("按ID更新失败", e);
            }
        }

        @Override
        public Integer update(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.UPDATE);
                return sqlSession.update(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("更新操作失败", e);
            }
        }

        @Override
        public Integer delete(E criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.DeleteByCriteriaSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
                return sqlSession.delete(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("删除操作失败", e);
            }
        }

        @Override
        public void deleteByLambda(LambdaQueryWrapper<E> wrapper) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.DeleteByLambdaSqlProvider().buildSql(wrapper, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
                sqlSession.delete(msId, wrapper);
            } catch (Exception e) {
                throw new RuntimeException("Lambda删除失败", e);
            }
        }

        @Override
        public void deleteByIds(List<String> array) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.DeleteByIdsSqlProvider().buildSql(array, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
                Map<String, List<String>> map = new HashMap<>();
                map.put("array", array);
                sqlSession.delete(msId, map);
            } catch (Exception e) {
                throw new RuntimeException("批量ID删除失败", e);
            }
        }

        @Override
        public Integer deleteByPrimaryKey(Serializable criteria) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession(true)) {
                String sql = new BaseMapper.DeleteSqlProvider().buildSql(criteria, this.table);
                String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
                return sqlSession.delete(msId, criteria);
            } catch (Exception e) {
                throw new RuntimeException("主键删除失败", e);
            }
        }

        public List<E> sqlQuery(String sql, Object param, Class<?> resultType) {
            return sqlQuery(sql, param, param != null ? param.getClass() : Map.class, resultType);
        }

        public List<E> sqlQuery(String sql, Object param, Class<?> paramType, Class<?> resultType) {
            try (SqlSession sqlSession = getSqlSessionFactory().openSession()) {
                String msId = execute(sqlSession.getConfiguration(), sql, paramType, resultType, SqlCommandType.SELECT);
                return sqlSession.selectList(msId, param);
            } catch (Exception e) {
                throw new RuntimeException("SQL查询执行失败", e);
            }
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

            // 批量插入的最大批次大小
            final int BATCH_SIZE = 500;
            int total;
            String sql = new BaseMapper.BatchInsertSqlProvider().buildSql(entities, this.table);

            try (SqlSession sqlSession = getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
                try {
                    String msId = execute(sqlSession.getConfiguration(), sql, table.getEntityClass(), int.class, SqlCommandType.INSERT);

                    // 分批处理数据
                    for (int i = 0; i < entities.size(); i++) {
                        sqlSession.insert(msId, entities.get(i));

                        // 每BATCH_SIZE条数据提交一次
                        if ((i + 1) % BATCH_SIZE == 0 || i == entities.size() - 1) {
                            sqlSession.flushStatements();
                        }
                    }

                    sqlSession.commit();
                    total = entities.size();
                } catch (Exception e) {
                    sqlSession.rollback();
                    throw new RuntimeException("批量插入失败", e);
                }
            }

            return total;
        }
    }

    /**
     * 执行 SQL，并返回生成的 MappedStatement ID。
     * 使用缓存机制减少MappedStatement的创建。
     */
    private String execute(Configuration configuration, String sql, Class<?> parameterType, Class<?> resultType, SqlCommandType sqlCommandType) {
        // 创建缓存键
        String cacheKey = sqlCommandType + "_" + sql + "_" + parameterType.getName() + "_" + resultType.getName();

        // 尝试从缓存获取MappedStatement ID
        String msId = cachedMappedStatements.get(cacheKey);
        if (msId != null && configuration.hasStatement(msId, false)) {
            return msId;
        }

        // 创建新的MappedStatement ID
        msId = sqlCommandType.toString() + "." + parameterType.getName() + "." + IDGenerator.nextStr();

        SqlSource sqlSource = configuration
                .getDefaultScriptingLanguageInstance()
                .createSqlSource(configuration, sql, parameterType);

        // 创建并注册新的MappedStatement
        newMappedStatement(configuration, msId, sqlSource, resultType, sqlCommandType);

        // 将新创建的MappedStatement ID放入缓存
        cachedMappedStatements.put(cacheKey, msId);

        return msId;
    }

    /**
     * 创建并注册新的 MappedStatement。
     */
    private void newMappedStatement(Configuration configuration, String msId, SqlSource sqlSource, Class<?> resultType, SqlCommandType sqlCommandType) {
        MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, sqlCommandType)
                .resultMaps(Collections.singletonList(
                        new ResultMap.Builder(configuration, "defaultResultMap", resultType, new ArrayList<>(0)).build()
                ))
                .build();
        configuration.addMappedStatement(ms);
    }
}