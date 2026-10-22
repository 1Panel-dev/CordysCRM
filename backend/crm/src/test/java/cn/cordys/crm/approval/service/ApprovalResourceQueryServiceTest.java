package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeLink;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalResourceQueryServiceTest {

    private final ApprovalResourceQueryService service = new ApprovalResourceQueryService();
    private final ApprovalInstanceService instances = mock(ApprovalInstanceService.class);
    private final ApprovalFlowService flows = mock(ApprovalFlowService.class);
    private final ExtApprovalInstanceMapper instanceMapper = mock(ExtApprovalInstanceMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalNodeLink> links = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalNode> nodes = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalTask> tasks = mock(BaseMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "approvalInstanceService", instances);
        ReflectionTestUtils.setField(service, "approvalFlowService", flows);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "nodeLinkMapper", links);
        ReflectionTestUtils.setField(service, "approvalNodeMapper", nodes);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", tasks);
    }

    @Test
    void emptyInputsDoNotQueryApprovalState() {
        assertEquals(Map.of(), service.getApprovingResourceFirstNodeApproved(List.of(), "org-1"));
        assertEquals(Map.of(), service.getApprovingResourceSubmitterIds(null));
        assertNull(service.getApprovingResourceSubmitterId(" "));
        verifyNoInteractions(instances, flows, instanceMapper, links, nodes, tasks);
    }

    @Test
    void submitterQueriesIgnoreBlankSubmittersAndMissingInstances() {
        ApprovalInstance active = instance("resource-1", "first");
        ApprovalInstance blankSubmitter = instance("resource-2", "first");
        blankSubmitter.setSubmitterId(" ");
        when(instances.getLatestInstances(List.of("resource-1", "resource-2", "resource-3")))
                .thenReturn(List.of(active, blankSubmitter));
        when(instances.getLatestInstances(List.of("resource-1"))).thenReturn(List.of(active));
        when(instances.getLatestInstances(List.of("resource-3"))).thenReturn(List.of());

        assertEquals(Map.of("resource-1", "submitter"),
                service.getApprovingResourceSubmitterIds(List.of("resource-1", "resource-2", "resource-3")));
        assertEquals("submitter", service.getApprovingResourceSubmitterId("resource-1"));
        assertNull(service.getApprovingResourceSubmitterId("resource-3"));
    }

    @ParameterizedTest
    @CsvSource({"false, 2, true", "false, 1, false", "true, 2, false"})
    void firstNodeChecksSignApprovalOnlyForTheCurrentRoundOfASingleApprover(
            boolean multiApprover, int signRound, boolean expected) {
        ApprovalInstance instance = instance("resource-1", "first");
        when(instances.getLatestInstances(List.of("resource-1"))).thenReturn(List.of(instance));
        when(nodes.selectListByLambda(any())).thenReturn(List.of(
                node("start", ApprovalNodeTypeEnum.START), node("condition", ApprovalNodeTypeEnum.CONDITION),
                node("first", ApprovalNodeTypeEnum.APPROVER), node("second", ApprovalNodeTypeEnum.APPROVER),
                node("end", ApprovalNodeTypeEnum.END)));
        when(links.selectListByLambda(any())).thenReturn(List.of(
                link("start", "condition"), link("condition", "first"), link("first", "second"), link("second", "end")));
        ApprovalTask signed = new ApprovalTask();
        signed.setInstanceId(instance.getId());
        signed.setNodeId("first");
        signed.setNodeRound(signRound);
        signed.setType(ApprovalTaskType.SN.name());
        signed.setStatus(ApprovalStatus.APPROVED.name());
        when(tasks.selectListByLambda(any())).thenReturn(List.of(signed));
        when(flows.isCurrentNodeMultiApprover("first", "submitter", "org-1")).thenReturn(multiApprover);
        when(instanceMapper.getNodeRound(instance.getId(), "first")).thenReturn(2);

        assertEquals(Map.of("resource-1", expected),
                service.getApprovingResourceFirstNodeApproved(List.of("resource-1"), "org-1"));
        verify(flows).isCurrentNodeMultiApprover("first", "submitter", "org-1");
    }

    @Test
    void laterApprovalNodeMarksTheFirstNodeCompleted() {
        when(instances.getLatestInstances(List.of("resource-1")))
                .thenReturn(List.of(instance("resource-1", "second")));
        when(nodes.selectListByLambda(any())).thenReturn(List.of(
                node("first", ApprovalNodeTypeEnum.APPROVER), node("second", ApprovalNodeTypeEnum.APPROVER)));
        when(links.selectListByLambda(any())).thenReturn(List.of(link("first", "second")));
        when(tasks.selectListByLambda(any())).thenReturn(List.of());

        assertEquals(Map.of("resource-1", true),
                service.getApprovingResourceFirstNodeApproved(List.of("resource-1"), "org-1"));
        verifyNoInteractions(flows, instanceMapper);
    }

    @Test
    void cyclicConditionNodesStillTerminate() {
        when(instances.getLatestInstances(List.of("resource-1")))
                .thenReturn(List.of(instance("resource-1", "first")));
        when(nodes.selectListByLambda(any())).thenReturn(List.of(
                node("first", ApprovalNodeTypeEnum.CONDITION), node("second", ApprovalNodeTypeEnum.DEFAULT)));
        when(links.selectListByLambda(any())).thenReturn(List.of(link("first", "second"), link("second", "first")));
        when(tasks.selectListByLambda(any())).thenReturn(List.of());

        assertEquals(Map.of("resource-1", true),
                service.getApprovingResourceFirstNodeApproved(List.of("resource-1"), "org-1"));
        verifyNoInteractions(flows, instanceMapper);
    }

    private ApprovalInstance instance(String resourceId, String nodeId) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-" + resourceId);
        instance.setResourceId(resourceId);
        instance.setSubmitterId("submitter");
        instance.setCurrentNodeId(nodeId);
        instance.setFlowVersionId("version-1");
        return instance;
    }

    private ApprovalNode node(String id, ApprovalNodeTypeEnum type) {
        ApprovalNode node = new ApprovalNode();
        node.setId(id);
        node.setNodeType(type.name());
        return node;
    }

    private ApprovalNodeLink link(String from, String to) {
        ApprovalNodeLink link = new ApprovalNodeLink();
        link.setFromNodeId(from);
        link.setToNodeId(to);
        return link;
    }
}
