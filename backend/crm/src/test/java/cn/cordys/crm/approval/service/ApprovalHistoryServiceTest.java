package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.response.ApprovalInstanceHistoryResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.mybatis.BaseMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalNode> nodeMapper = mock(BaseMapper.class);
    private final ApprovalResourceService resources = mock(ApprovalResourceService.class);
    private final ApprovalInstanceService instances = mock(ApprovalInstanceService.class);
    private final ApprovalCommentService comments = mock(ApprovalCommentService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "approvalNodeMapper", nodeMapper);
        ReflectionTestUtils.setField(service, "approvalResourceService", resources);
        ReflectionTestUtils.setField(service, "approvalInstanceService", instances);
        ReflectionTestUtils.setField(service, "approvalCommentService", comments);
    }

    @Test
    void listKeepsQueryOrderAndSkipsLatestBeforeFilteringStatusAndPermission() {
        ApprovalInstance latest = instance("latest", ApprovalStatus.REVOKED, "latest-node");
        ApprovalInstance approved = instance("approved", ApprovalStatus.APPROVED, "end");
        ApprovalInstance revoked = instance("revoked", ApprovalStatus.REVOKED, "revoked-node");
        ApprovalInstance rejected = instance("rejected", ApprovalStatus.UNAPPROVED, "old-node-SN1");
        ApprovalInstance forbidden = instance("forbidden", ApprovalStatus.UNAPPROVED, "forbidden-node");
        ApprovalInstance approving = instance("approving", ApprovalStatus.APPROVING, "active-node");
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG))
                .thenReturn(List.of(latest, approved, revoked, rejected, forbidden, approving));
        when(resources.hasInstanceViewPermission(approved, USER, ORG)).thenReturn(true);
        when(resources.hasInstanceViewPermission(rejected, USER, ORG)).thenReturn(true);
        when(nodeMapper.selectByIds(List.of("end", "old-node")))
                .thenReturn(List.of(node("end", "结束"), node("old-node", "旧版区域审批")));

        List<ApprovalInstanceHistoryResponse> result = service.getHistoryInstances(RESOURCE, USER, ORG);

        assertEquals(List.of("approved", "rejected"), result.stream()
                .map(ApprovalInstanceHistoryResponse::getId).toList());
        assertEquals(ApprovalStatus.APPROVED.name(), result.getFirst().getApprovalStatus());
        assertEquals("old-node-SN1", result.getLast().getCurrentNodeId());
        assertEquals("旧版区域审批", result.getLast().getCurrentNodeName());
        verify(resources).hasInstanceViewPermission(forbidden, USER, ORG);
        verify(resources, never()).hasInstanceViewPermission(latest, USER, ORG);
        verify(resources, never()).hasInstanceViewPermission(revoked, USER, ORG);
        verify(resources, never()).hasInstanceViewPermission(approving, USER, ORG);
        verifyNoInteractions(instances, comments);
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"APPROVED", "UNAPPROVED"})
    void listDoesNotExposeLatestCompletedInstance(ApprovalStatus latestStatus) {
        ApprovalInstance latest = instance("latest", latestStatus, "latest-node");
        ApprovalInstance previous = instance("previous", ApprovalStatus.UNAPPROVED, null);
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG)).thenReturn(List.of(latest, previous));
        when(resources.hasInstanceViewPermission(previous, USER, ORG)).thenReturn(true);

        List<ApprovalInstanceHistoryResponse> result = service.getHistoryInstances(RESOURCE, USER, ORG);

        assertEquals(List.of("previous"), result.stream().map(ApprovalInstanceHistoryResponse::getId).toList());
        assertNull(result.getFirst().getCurrentNodeName());
        verifyNoInteractions(nodeMapper, instances, comments);
    }

    @Test
    void listDoesNotReadNodesForInstancesWithoutViewPermission() {
        ApprovalInstance previous = instance("previous", ApprovalStatus.UNAPPROVED, "old-node");
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG))
                .thenReturn(List.of(instance("latest", ApprovalStatus.APPROVING, "new-node"), previous));

        assertTrue(service.getHistoryInstances(RESOURCE, USER, ORG).isEmpty());

        verify(resources).hasInstanceViewPermission(previous, USER, ORG);
        verifyNoInteractions(nodeMapper, instances, comments);
    }

    @Test
    void emptyOrganizationResultDoesNotReadInstanceDetails() {
        when(instanceMapper.selectInstancesByResourceId(RESOURCE, ORG)).thenReturn(List.of());

        assertTrue(service.getHistoryInstances(RESOURCE, USER, ORG).isEmpty());

        verifyNoInteractions(resources, nodeMapper, instances, comments);
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"APPROVED", "UNAPPROVED"})
    void detailUsesRequestedInstanceAndItsCommentPermission(ApprovalStatus status) {
        ApprovalInstance historical = instance("historical", status, "old-node");
        historical.setFlowVersionId("old-version");
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId(historical.getId());
        when(instanceMapper.selectInstanceById(historical.getId(), ORG)).thenReturn(historical);
        when(resources.hasInstanceViewPermission(historical, USER, ORG)).thenReturn(true);
        when(instances.getHistoricalApprovalInstanceDetail(historical, ORG)).thenReturn(detail);
        when(comments.hasCommentPermission(historical.getId(), USER, ORG)).thenReturn(true);

        ApprovalInstanceDetail result = service.getHistoryInstanceDetail(historical.getId(), USER, ORG);

        assertSame(detail, result);
        assertEquals("historical", result.getId());
        assertTrue(result.isCommentable());
        verify(instances).getHistoricalApprovalInstanceDetail(historical, ORG);
        verify(comments).hasCommentPermission(historical.getId(), USER, ORG);
        verifyNoInteractions(nodeMapper);
    }

    @Test
    void missingOrOtherOrganizationInstanceReturnsNotFound() {
        GenericException error = assertThrows(GenericException.class,
                () -> service.getHistoryInstanceDetail("other-instance", USER, ORG));

        assertSame(CrmHttpResultCode.NOT_FOUND, error.getErrorCode());
        verify(instanceMapper).selectInstanceById("other-instance", ORG);
        verifyNoInteractions(resources, instances, comments, nodeMapper);
    }

    @Test
    void detailDeniesUnrelatedUserBeforeBuildingRecordsOrCheckingComments() {
        ApprovalInstance historical = instance("historical", ApprovalStatus.UNAPPROVED, "old-node");
        when(instanceMapper.selectInstanceById(historical.getId(), ORG)).thenReturn(historical);

        GenericException error = assertThrows(GenericException.class,
                () -> service.getHistoryInstanceDetail(historical.getId(), USER, ORG));

        assertSame(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(instances, comments, nodeMapper);
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"NONE", "PENDING", "APPROVING", "REVOKED"})
    void detailDoesNotExposeUnfinishedOrRevokedInstances(ApprovalStatus status) {
        ApprovalInstance instance = instance("not-historical", status, "node");
        when(instanceMapper.selectInstanceById(instance.getId(), ORG)).thenReturn(instance);

        GenericException error = assertThrows(GenericException.class,
                () -> service.getHistoryInstanceDetail(instance.getId(), USER, ORG));

        assertSame(CrmHttpResultCode.NOT_FOUND, error.getErrorCode());
        verifyNoInteractions(resources, instances, comments, nodeMapper);
    }

    @Test
    void mapperScopesBothQueriesToOrganizationAndSortsBeforeHistoryFiltering() throws Exception {
        Configuration configuration = new Configuration();
        String resource = ExtApprovalInstanceMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = ExtApprovalInstanceMapper.class.getName() + ".";
        BoundSql listSql = configuration.getMappedStatement(namespace + "selectInstancesByResourceId")
                .getBoundSql(Map.of("resourceId", RESOURCE, "organizationId", ORG));
        String sql = listSql.getSql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("join approval_flow_version afv on afv.id = ai.flow_version_id"));
        assertTrue(sql.contains("where ai.resource_id = ? and afv.organization_id = ?"));
        assertTrue(sql.contains("order by ai.submit_time desc, ai.id desc"));
        assertEquals(List.of("resourceId", "organizationId"), listSql.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList());
        BoundSql detailSql = configuration.getMappedStatement(namespace + "selectInstanceById")
                .getBoundSql(Map.of("instanceId", "historical", "organizationId", ORG));
        assertTrue(detailSql.getSql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT)
                .contains("where ai.id = ? and afv.organization_id = ?"));
        assertEquals(List.of("instanceId", "organizationId"), detailSql.getParameterMappings().stream()
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

    private ApprovalNode node(String id, String name) {
        ApprovalNode node = new ApprovalNode();
        node.setId(id);
        node.setName(name);
        return node;
    }
}
