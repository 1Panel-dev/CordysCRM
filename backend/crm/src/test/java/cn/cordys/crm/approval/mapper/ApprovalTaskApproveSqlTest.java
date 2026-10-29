package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalTask;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalTaskApproveSqlTest {

    @Test
    void instanceLockUsesTheTasksStoredInstanceBeforeTaskUpdates() throws Exception {
        Configuration configuration = new Configuration();
        String resource = ExtApprovalInstanceMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
        BoundSql sql = configuration.getMappedStatement(
                        ExtApprovalInstanceMapper.class.getName() + ".selectByTaskIdForUpdate")
                .getBoundSql(Map.of("taskId", "task-1"));

        assertEquals("SELECT ai.* FROM approval_instance ai "
                        + "WHERE ai.id = (SELECT instance_id FROM approval_task WHERE id = ?) FOR UPDATE",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("taskId"), sql.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList());
    }

    @Test
    void approvalUpdateAtomicallyRequiresPendingStatusAndTheSameApprover() throws Exception {
        Configuration configuration = new Configuration();
        String resource = ExtApprovalTaskMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
        ApprovalTask task = new ApprovalTask();
        task.setId("task-1");
        task.setUpdateUser("approver-1");
        task.setUpdateTime(123L);
        BoundSql boundSql = configuration.getMappedStatement(
                        ExtApprovalTaskMapper.class.getName() + ".approvePendingTask")
                .getBoundSql(Map.of("approvalTask", task));
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();

        assertTrue(sql.contains("SET status = 'APPROVED', action = 'APPROVE'"));
        assertTrue(sql.contains("WHERE id = ? AND approver_id = ? AND status = 'APPROVING' AND type != 'CC'"));
        assertEquals(List.of("approvalTask.updateTime", "approvalTask.updateUser", "approvalTask.id",
                        "approvalTask.updateUser"),
                boundSql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
    }
}
