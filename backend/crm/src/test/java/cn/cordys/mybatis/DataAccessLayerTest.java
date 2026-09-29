package cn.cordys.mybatis;

import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.persistence.Table;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 不启动应用或数据库，直接验证 DAL 注册的真实动态 SQL 和会话边界。 */
class DataAccessLayerTest {
    private Configuration configuration;
    private SqlSession session;
    private BaseMapper<Entity> mapper;
    private final List<String> queryIds = new ArrayList<>();
    private final List<String> queries = new ArrayList<>();

    @BeforeEach
    void setUp() {
        configuration = new Configuration();
        session = mock(SqlSession.class);
        when(session.getConfiguration()).thenReturn(configuration);
        when(session.selectList(anyString(), any())).thenAnswer(call -> {
            String id = call.getArgument(0);
            queryIds.add(id);
            queries.add(sql(id, call.getArgument(1)));
            return List.of();
        });
        mapper = DataAccessLayer.with(Entity.class, session);
    }

    @Test
    void cacheSeparatesSortsAndColumnsButReusesIdenticalSql() {
        mapper.selectAll("name ASC");
        mapper.selectAll("name DESC");
        mapper.selectAll("name ASC");
        mapper.selectByColumn("id", new String[]{"first"});
        mapper.selectByColumn("name", new String[]{"second"});

        assertTrue(queries.get(0).endsWith("ORDER BY name ASC"));
        assertTrue(queries.get(1).endsWith("ORDER BY name DESC"));
        assertEquals(queryIds.get(0), queryIds.get(2));
        assertNotEquals(queryIds.get(0), queryIds.get(1));
        assertTrue(queries.get(3).contains("WHERE id IN"));
        assertTrue(queries.get(4).contains("WHERE name IN"));
        assertNotEquals(queryIds.get(3), queryIds.get(4));
    }

    @Test
    void executorKeepsItsOriginalSessionAndConfiguration() {
        SqlSession otherSession = mock(SqlSession.class);
        Configuration otherConfiguration = new Configuration();
        when(otherSession.getConfiguration()).thenReturn(otherConfiguration);
        DataAccessLayer.with(Entity.class, otherSession);

        mapper.selectByPrimaryKey("first");

        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        verify(session).selectOne(statement.capture(), eq("first"));
        verify(otherSession, never()).selectOne(anyString(), any());
        assertTrue(configuration.hasStatement(statement.getValue(), false));
        assertFalse(otherConfiguration.hasStatement(statement.getValue(), false));
    }

    @Test
    void emptyInMatchesNothingEvenWhenOtherConditionsExist() {
        List<String> deletes = new ArrayList<>();
        when(session.delete(anyString(), any())).thenAnswer(call -> {
            deletes.add(sql(call.getArgument(0), call.getArgument(1)));
            return 0;
        });

        for (List<?> values : Arrays.<List<?>>asList(null, List.of())) {
            LambdaQueryWrapper<Entity> wrapper = new LambdaQueryWrapper<Entity>()
                    .in(Entity::getId, values).eq(Entity::getName, "match");
            mapper.selectListByLambda(wrapper);
            mapper.deleteByLambda(wrapper);
        }

        assertEquals(2, queries.size());
        assertEquals(2, deletes.size());
        for (String sql : queries) {
            assertTrue(sql.contains("1 = 0"));
            assertTrue(sql.contains("name = ?"));
        }
        for (String sql : deletes) {
            assertTrue(sql.contains("1 = 0"));
            assertTrue(sql.contains("name = ?"));
        }
    }

    @Test
    void emptyDeletesAreRejectedBeforeExecutingSql() {
        assertThrows(IllegalArgumentException.class, () -> mapper.delete(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.delete(new Entity()));
        assertThrows(IllegalArgumentException.class, () -> mapper.deleteByLambda(new LambdaQueryWrapper<>()));
        verify(session, never()).delete(anyString(), any());

        Entity entity = entity("first");
        mapper.delete(entity);
        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        verify(session).delete(statement.capture(), same(entity));
        assertTrue(sql(statement.getValue(), entity).contains("WHERE `id` = ?"));
    }

    @Test
    void rawBatchUsesCallerSessionWithoutCommittingOrClosingIt() {
        Entity first = entity("first");
        Entity second = entity("second");
        when(session.insert(anyString(), any())).thenReturn(1);

        assertEquals(0, mapper.batchInsert(null));
        assertEquals(0, mapper.batchInsert(List.of()));
        assertEquals(2, mapper.batchInsert(List.of(first, second)));
        verify(session).insert(anyString(), same(first));
        verify(session).insert(anyString(), same(second));
        verify(session, never()).commit();
        verify(session, never()).close();
    }

    @Test
    void springBatchUsesItsOwnFactoryAndClearsTheQueryCache() {
        SqlSession batchSession = mock(SqlSession.class);
        SqlSessionTemplate template = springSession(batchSession);
        BaseMapper<Entity> springMapper = DataAccessLayer.with(Entity.class, template);

        assertEquals(2, springMapper.batchInsert(List.of(entity("first"), entity("second"))));

        verify(batchSession, times(2)).insert(anyString(), any(Entity.class));
        verify(batchSession).flushStatements();
        verify(batchSession).commit();
        verify(template).clearCache();
        verify(batchSession).close();
    }

    @Test
    void batchFailurePreservesItsCauseAndClosesTheBatchSession() {
        SqlSession batchSession = mock(SqlSession.class);
        SqlSessionTemplate template = springSession(batchSession);
        BaseMapper<Entity> springMapper = DataAccessLayer.with(Entity.class, template);
        IllegalStateException failure = new IllegalStateException("batch failed");
        doThrow(failure).when(batchSession).flushStatements();

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> springMapper.batchInsert(List.of(entity("first"))));

        assertSame(failure, thrown.getCause());
        verify(batchSession, never()).commit();
        verify(batchSession).close();
    }

    private SqlSessionTemplate springSession(SqlSession batchSession) {
        SqlSessionFactory factory = mock(SqlSessionFactory.class);
        SqlSessionTemplate template = mock(SqlSessionTemplate.class);
        when(template.getConfiguration()).thenReturn(configuration);
        when(template.getSqlSessionFactory()).thenReturn(factory);
        when(factory.openSession(ExecutorType.BATCH, false)).thenReturn(batchSession);
        return template;
    }

    private String sql(String id, Object parameter) {
        return configuration.getMappedStatement(id).getBoundSql(parameter).getSql().replaceAll("\\s+", " ").trim();
    }

    private Entity entity(String id) {
        Entity entity = new Entity();
        entity.id = id;
        return entity;
    }

    @Table(name = "dal_test_entity")
    public static class Entity {
        public String id;
        public String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
