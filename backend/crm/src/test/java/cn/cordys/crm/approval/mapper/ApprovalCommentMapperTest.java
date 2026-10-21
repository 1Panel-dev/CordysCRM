package cn.cordys.crm.approval.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalCommentMapperTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        String resource = ExtApprovalCommentMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void selectInstanceRequiresRequestedInstanceAndVersionOrganization() {
        assertSql("selectInstance", Map.of("instanceId", "instance-a", "organizationId", "org-a"),
                List.of("instance-a", "org-a"),
                "join approval_flow_version afv on afv.id = ai.flow_version_id",
                "where ai.id = ?", "and afv.organization_id = ?");
    }

    @Test
    void mentionTodosKeepEveryMentionAndRestrictUserAndBothOrganizations() {
        Map<String, Object> parameters = Map.of("userId", "user-a", "organizationId", "org-a");
        assertSql("selectMentionTodos", parameters, List.of("user-a", "org-a", "org-a"),
                "from approval_comment_mention m", "join approval_comment c on c.id = m.comment_id",
                "join approval_instance ai on ai.id = c.instance_id",
                "join approval_flow_version afv on afv.id = ai.flow_version_id",
                "where m.user_id = ?", "and c.organization_id = ?", "and afv.organization_id = ?",
                "m.id as commentmentionid", "c.id as commentid", "m.create_time as mentiontime",
                "order by m.create_time desc, m.id desc");
        String sql = configuration.getMappedStatement(ExtApprovalCommentMapper.class.getName() + ".selectMentionTodos")
                .getBoundSql(parameters).getSql().toLowerCase(Locale.ROOT);
        assertFalse(sql.contains("distinct"));
        assertFalse(sql.contains("group by"));
        assertFalse(sql.contains("approval_status ="));
        assertFalse(sql.contains("approval_task"));
    }

    @Test
    void mentionTodosBindStandardAndCustomFormTypeAndKeywordFilters() {
        for (String resourceType : List.of("contract", "custom-form-a")) {
            assertSql("selectMentionTodos", Map.of("userId", "user-a", "organizationId", "org-a",
                            "resourceType", resourceType, "keyword", "合同"),
                    List.of("user-a", "org-a", "org-a", resourceType, "合同"),
                    "and ai.type = ?", "and ai.resource_name like concat('%', ?, '%')");
        }
    }

    @Test
    void mentionInstanceRequiresExactMentionCurrentUserAndBothOrganizations() {
        assertSql("selectMentionInstance", Map.of("mentionId", "mention-a", "userId", "user-a", "organizationId", "org-a"),
                List.of("mention-a", "user-a", "org-a", "org-a"),
                "select ai.*", "join approval_comment c on c.id = m.comment_id",
                "join approval_instance ai on ai.id = c.instance_id",
                "join approval_flow_version afv on afv.id = ai.flow_version_id",
                "where m.id = ?", "and m.user_id = ?", "and c.organization_id = ?", "and afv.organization_id = ?");
    }

    @Test
    void mentionPermissionRequiresExistingCommentInRequestedInstanceAndOrganization() {
        assertSql("existsMention",
                Map.of("instanceId", "instance-a", "organizationId", "org-a", "userId", "user-a"),
                List.of("instance-a", "org-a", "user-a"),
                "from approval_comment_mention m", "join approval_comment c on c.id = m.comment_id",
                "where c.instance_id = ?", "and c.organization_id = ?", "and m.user_id = ?");
    }

    @Test
    void commentWriteLocksOnlyRequestedCommentWithinOrganization() {
        assertSql("selectCommentForUpdate", Map.of("id", "comment-a", "organizationId", "org-a"),
                List.of("comment-a", "org-a"),
                "from approval_comment c", "where c.id = ?", "and c.organization_id = ?", "for update");
    }

    @Test
    void deletesRequireOrganizationAndExplicitCommentIds() {
        Map<String, Object> parameters = Map.of("organizationId", "org-a", "instanceId", "instance-a",
                "commentIds", List.of("comment-a", "comment-b"));
        assertSql("deleteComments", parameters, List.of("org-a", "instance-a", "comment-a", "comment-b"),
                "delete from approval_comment", "where organization_id = ?", "and instance_id = ?", "and id in");
        assertSql("deleteMentions", parameters, List.of("org-a", "comment-a", "comment-b"),
                "delete m from approval_comment_mention m", "join approval_comment c on c.id = m.comment_id",
                "where c.organization_id = ?", "and m.comment_id in");
    }

    @Test
    void listAndChildrenKeepParentAndInstanceBoundariesWithScopedReplyCounts() {
        Map<String, Object> parameters = Map.of("organizationId", "org-a", "instanceId", "instance-a",
                "parentIds", List.of("comment-a", "comment-b"));
        for (String statement : List.of("selectList", "selectChildren")) {
            boolean children = statement.equals("selectChildren");
            assertSql(statement, parameters,
                    children ? List.of("org-a", "instance-a", "comment-a", "comment-b")
                            : List.of("org-a", "instance-a"),
                    "where c.organization_id = ?", "and c.instance_id = ?",
                    children ? "and c.parent_id in" : "and c.parent_id is null",
                    "order by c.create_time desc, c.id desc",
                    "select count(1) from approval_comment reply where reply.parent_id = c.id",
                    "and reply.instance_id = c.instance_id",
                    "and reply.organization_id = c.organization_id) as reply_count");
        }
    }

    private static void assertSql(String statement, Map<String, Object> parameters,
            List<String> expectedValues, String... requiredPredicates) {
        BoundSql boundSql = configuration.getMappedStatement(ExtApprovalCommentMapper.class.getName() + "." + statement)
                .getBoundSql(parameters);
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        for (String predicate : requiredPredicates) {
            assertTrue(sql.contains(predicate), "缺少 SQL 边界：" + predicate);
        }
        List<Object> actualValues = boundSql.getParameterMappings().stream().map(mapping -> {
            String property = mapping.getProperty();
            return boundSql.hasAdditionalParameter(property) ? boundSql.getAdditionalParameter(property)
                    : configuration.newMetaObject(parameters).getValue(property);
        }).toList();
        assertEquals(expectedValues, actualValues, "参数应正确绑定组织、实例、用户及目标评论");
    }
}
