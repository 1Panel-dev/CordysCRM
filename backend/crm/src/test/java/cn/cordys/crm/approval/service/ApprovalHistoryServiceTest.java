package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalHistoryServiceTest {

    private static final String RESOURCE = "resource-1";
    private static final String USER = "user-1";
    private static final String ORG = "org-1";

    private final ApprovalHistoryService service = new ApprovalHistoryService();
    private final ExtApprovalInstanceMapper instanceMapper = mock(ExtApprovalInstanceMapper.class);
    private final ApprovalResourceService resources = mock(ApprovalResourceService.class);
    private final ApprovalInstanceService instances = mock(ApprovalInstanceService.class);
    private final ApprovalCommentService comments = mock(ApprovalCommentService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "approvalResourceService", resources);
        ReflectionTestUtils.setField(service, "approvalInstanceService", instances);
        ReflectionTestUtils.setField(service, "approvalCommentService", comments);
    }

    @Test
    void detailsReturnOldestFirstUsingEachInstanceVersionAndCommentPermission() {
        ApprovalInstance latest = instance("latest", ApprovalStatus.APPROVING, "latest-node");
        ApprovalInstance recent = instance("recent", ApprovalStatus.APPROVED, "end");
        ApprovalInstance oldest = instance("oldest", ApprovalStatus.UNAPPROVED, "old-node-SN1");
        ApprovalInstanceDetail recentDetail = new ApprovalInstanceDetail();
        recentDetail.setId(recent.getId());
        ApprovalInstanceDetail oldestDetail = new ApprovalInstanceDetail();
        oldestDetail.setId(oldest.getId());
        oldestDetail.setCommentable(true);
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG))
                .thenReturn(List.of(latest, recent, oldest));
        when(resources.hasInstanceViewPermission(recent, USER, ORG)).thenReturn(true);
        when(resources.hasInstanceViewPermission(oldest, USER, ORG)).thenReturn(true);
        when(instances.getHistoricalApprovalInstanceDetail(recent, ORG)).thenReturn(recentDetail);
        when(instances.getHistoricalApprovalInstanceDetail(oldest, ORG)).thenReturn(oldestDetail);
        when(comments.hasCommentPermission(recent.getId(), USER, ORG)).thenReturn(true);
        when(comments.hasCommentPermission(oldest.getId(), USER, ORG)).thenReturn(false);

        List<ApprovalInstanceDetail> result = service.getHistoryInstanceDetail(RESOURCE, USER, ORG);

        assertEquals(List.of("oldest", "recent"), result.stream().map(ApprovalInstanceDetail::getId).toList());
        assertSame(oldestDetail, result.getFirst());
        assertSame(recentDetail, result.getLast());
        assertFalse(result.getFirst().isCommentable());
        assertTrue(result.getLast().isCommentable());
        assertEquals("version-recent", recent.getFlowVersionId());
        assertEquals("version-oldest", oldest.getFlowVersionId());
        verify(instances).getHistoricalApprovalInstanceDetail(recent, ORG);
        verify(instances).getHistoricalApprovalInstanceDetail(oldest, ORG);
        verify(comments).hasCommentPermission(recent.getId(), USER, ORG);
        verify(comments).hasCommentPermission(oldest.getId(), USER, ORG);
        verify(resources, never()).hasInstanceViewPermission(latest, USER, ORG);
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = { "APPROVING", "APPROVED", "UNAPPROVED", "REVOKED" })
    void detailsExcludeLatestBeforeFilteringStatus(ApprovalStatus latestStatus) {
        ApprovalInstance latest = instance("latest", latestStatus, "latest-node");
        ApprovalInstance previous = instance("previous", ApprovalStatus.UNAPPROVED, "old-node");
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId(previous.getId());
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG)).thenReturn(List.of(latest, previous));
        when(resources.hasInstanceViewPermission(previous, USER, ORG)).thenReturn(true);
        when(instances.getHistoricalApprovalInstanceDetail(previous, ORG)).thenReturn(detail);

        List<ApprovalInstanceDetail> result = service.getHistoryInstanceDetail(RESOURCE, USER, ORG);

        assertEquals(List.of(detail), result);
        verify(resources, never()).hasInstanceViewPermission(latest, USER, ORG);
        verify(instances, never()).getHistoricalApprovalInstanceDetail(latest, ORG);
        verify(comments, never()).hasCommentPermission(latest.getId(), USER, ORG);
    }

    @Test
    void detailsDoNotReadRecordsOrCommentsForInstancesWithoutViewPermission() {
        ApprovalInstance previous = instance("previous", ApprovalStatus.UNAPPROVED, "old-node");
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG))
                .thenReturn(List.of(instance("latest", ApprovalStatus.APPROVING, "new-node"), previous));

        assertTrue(service.getHistoryInstanceDetail(RESOURCE, USER, ORG).isEmpty());

        verify(resources).hasInstanceViewPermission(previous, USER, ORG);
        verifyNoInteractions(instances, comments);
    }

    @Test
    void missingResourceOrOtherOrganizationReturnsEmptyDetails() {
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG)).thenReturn(List.of());
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, "other-org")).thenReturn(List.of());

        assertTrue(service.getHistoryInstanceDetail(RESOURCE, USER, ORG).isEmpty());
        assertTrue(service.getHistoryInstanceDetail(RESOURCE, USER, "other-org").isEmpty());

        verify(instanceMapper).selectInstancesByResourceId(RESOURCE, ORG);
        verify(instanceMapper).selectInstancesByResourceId(RESOURCE, "other-org");
        verifyNoInteractions(resources, instances, comments);
    }

    @Test
    void onlyCurrentInstanceReturnsEmptyDetails() {
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG))
                .thenReturn(List.of(instance("latest", ApprovalStatus.APPROVED, "end")));

        assertTrue(service.getHistoryInstanceDetail(RESOURCE, USER, ORG).isEmpty());

        verifyNoInteractions(resources, instances, comments);
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = { "NONE", "PENDING", "APPROVING", "REVOKED" })
    void detailsExcludeUnfinishedOrRevokedInstances(ApprovalStatus status) {
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG))
                .thenReturn(List.of(instance("latest", ApprovalStatus.APPROVING, "new-node"),
                        instance("not-historical", status, "old-node")));

        assertTrue(service.getHistoryInstanceDetail(RESOURCE, USER, ORG).isEmpty());

        verifyNoInteractions(resources, instances, comments);
    }

    @Test
    void mapperScopesQueryToOrganizationAndSortsStablyBeforeHistoryFiltering() throws Exception {
        Configuration configuration = new Configuration();
        String resource = ExtApprovalInstanceMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        BoundSql listSql = configuration.getMappedStatement(
                ExtApprovalInstanceMapper.class.getName() + ".selectInstancesByResourceId")
                .getBoundSql(Map.of("resourceId", RESOURCE, "organizationId", ORG));
        String sql = listSql.getSql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("join approval_flow_version afv on afv.id = ai.flow_version_id"));
        assertTrue(sql.contains("where ai.resource_id = ? and afv.organization_id = ?"));
        assertTrue(sql.contains("order by ai.submit_time desc, ai.id desc"));
        assertEquals(List.of("resourceId", "organizationId"), listSql.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList());
    }

    private ApprovalInstance instance(String id, ApprovalStatus status, String nodeId) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(id);
        instance.setResourceId(RESOURCE);
        instance.setFlowVersionId("version-" + id);
        instance.setApprovalStatus(status.name());
        instance.setCurrentNodeId(nodeId);
        return instance;
    }
}
