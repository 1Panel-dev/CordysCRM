package cn.cordys.crm.approval.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验待办列表和统计排除已撤销实例，并保留任务、节点及类型过滤。
 */
class ApprovalTodoPendingSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        String resource = ExtApprovalTaskMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "contract", "custom-form-a" })
    void pendingListExcludesRevokedInstancesForEveryResourceType(String resourceType) {
        Map<String, Object> parameters = new HashMap<>(Map.of(
                "approverId", "approver-a", "pendingStatus", "APPROVING", "resourceName", ""));
        parameters.put("resourceType", resourceType);
        BoundSql sql = configuration.getMappedStatement(ExtApprovalTaskMapper.class.getName() + ".selectPendingTasks")
                .getBoundSql(parameters);

        assertPendingScope(sql);
        boolean filtered = resourceType != null && !resourceType.isEmpty();
        assertEquals(filtered, sql.getSql().contains("AND ai.type = ?"));
        assertEquals(filtered ? List.of("approverId", "pendingStatus", "resourceType")
                        : List.of("approverId", "pendingStatus"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals(filtered ? List.of("approver-a", "APPROVING", resourceType) : List.of("approver-a", "APPROVING"),
                sql.getParameterMappings().stream().map(mapping -> parameters.get(mapping.getProperty())).toList());
    }

    @Test
    void pendingCountExcludesRevokedInstancesAcrossAllResourceTypes() {
        Map<String, Object> parameters = Map.of("approverId", "approver-a", "pendingStatus", "APPROVING");
        BoundSql sql = configuration.getMappedStatement(ExtApprovalTaskMapper.class.getName() + ".countPendingGroupByType")
                .getBoundSql(parameters);

        assertPendingScope(sql);
        assertTrue(sql.getSql().contains("GROUP BY ai.type"));
        assertFalse(sql.getSql().contains("AND ai.type = ?"));
        assertEquals(List.of("approverId", "pendingStatus"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals(List.of("approver-a", "APPROVING"),
                sql.getParameterMappings().stream().map(mapping -> parameters.get(mapping.getProperty())).toList());
    }

    private void assertPendingScope(BoundSql boundSql) {
        String sql = boundSql.getSql().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        assertTrue(sql.contains("AI.APPROVAL_STATUSNOTIN('APPROVED','UNAPPROVED','REVOKED')"),
                "待办列表和统计均应排除已通过、已驳回和已撤销实例");
        assertTrue(sql.contains("AT.APPROVER_ID=?"));
        assertTrue(sql.contains("AT.STATUS=?"));
        assertTrue(sql.contains("AI.CURRENT_NODE_ID=AT.NODE_ID"));
    }
}
