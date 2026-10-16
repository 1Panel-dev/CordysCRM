package cn.cordys.crm.approval.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 校验真实 Mapper SQL 的轮次范围及实例、节点边界，不依赖数据库环境。
 */
class ApprovalInstanceCleanupSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        String resource = ExtApprovalInstanceMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    static Stream<Arguments> cleanupStatements() {
        return Stream.of(
                new String[]{"batchClearNotApprovingTask", """
                        update approval_task set node_round = -1
                        where instance_id = ? AND node_id = ? and node_round BETWEEN 0 AND ? and status != 'APPROVING'
                        """},
                new String[]{"batchClearApprovingTask", """
                        update approval_task set status = 'NONE', node_round = -1
                        where instance_id = ? AND node_id = ? and node_round BETWEEN 0 AND ? and status = 'APPROVING'
                        """},
                new String[]{"batchClearRecord", """
                        update approval_record set node_round = -1
                        where instance_id = ? AND node_id = ? and node_round BETWEEN 0 AND ?
                        """}
        ).flatMap(statement -> Stream.of(0, 2)
                .map(round -> Arguments.of(statement[0], statement[1], round)));
    }

    @ParameterizedTest
    @MethodSource("cleanupStatements")
    void cleanupInvalidatesEveryEffectiveRoundWithinInstanceAndNode(String statement, String expectedSql, int maxRound) {
        assertSql(statement, expectedSql, maxRound);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void loseApprovingTaskOnlyStopsSpecifiedRound(int round) {
        assertSql("loseApprovingTask", """
                update approval_task set status = 'NONE'
                where instance_id = ? AND node_id = ? and node_round = ? and status = 'APPROVING'
                """, round);
    }

    private void assertSql(String statement, String expectedSql, int round) {
        Map<String, Object> parameters = Map.of("instanceId", "instance-a", "nodeId", "node-b", "nodeRound", round);
        BoundSql sql = configuration.getMappedStatement(ExtApprovalInstanceMapper.class.getName() + "." + statement)
                .getBoundSql(parameters);
        assertEquals(expectedSql.replaceAll("\\s+", " ").trim(), sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("instanceId", "nodeId", "nodeRound"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals(List.of("instance-a", "node-b", round),
                sql.getParameterMappings().stream().map(mapping -> parameters.get(mapping.getProperty())).toList());
    }
}
