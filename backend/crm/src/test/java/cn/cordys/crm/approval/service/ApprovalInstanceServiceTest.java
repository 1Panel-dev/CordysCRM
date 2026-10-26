package cn.cordys.crm.approval.service;

import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalReturnBackRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.approval.dto.ApprovalTaskNode;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalInstanceServiceTest {

    @ParameterizedTest
    @CsvSource({ "0,true", "3,true", "0,false", "3,false" })
    @SuppressWarnings("unchecked")
    void instanceDetailCountsResourceCommentsForRequestedOrLatestInstance(long commentCount, boolean latestInstance) {
        ApprovalInstanceService service = new ApprovalInstanceService();
        BaseMapper<ApprovalInstance> instanceMapper = mock(BaseMapper.class);
        BaseMapper<ApprovalComment> commentMapper = mock(BaseMapper.class);
        UserExtendService userExtendService = mock(UserExtendService.class);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "approvalCommentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalRecordMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalAddSignTaskMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalReturnBackRecordMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalNodeMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "userExtendService", userExtendService);
        ApprovalInstance previous = new ApprovalInstance();
        previous.setId("instance-previous");
        previous.setResourceId("resource-1");
        previous.setSubmitTime(1L);
        previous.setSubmitterId("submitter");
        previous.setCurrentNodeId("");
        ApprovalInstance latest = new ApprovalInstance();
        latest.setId("instance-latest");
        latest.setResourceId("resource-1");
        latest.setSubmitTime(2L);
        latest.setSubmitterId("submitter");
        latest.setCurrentNodeId("");
        when(instanceMapper.selectListByLambda(any())).thenReturn(List.of(latest, previous));
        when(userExtendService.getAllUserSimpleMap()).thenReturn(Map.of());
        when(commentMapper.countByExample(any())).thenReturn(commentCount);

        ApprovalInstance requested = latestInstance ? latest : previous;
        ApprovalInstanceDetail detail = latestInstance ? service.getLatestApprovalInstanceDetail("resource-1", "org-1")
                : service.getApprovalInstanceDetail(requested, "org-1");

        assertEquals(requested.getId(), detail.getId());
        assertEquals(commentCount, detail.getCommentCount());
        ArgumentCaptor<ApprovalComment> criteria = ArgumentCaptor.forClass(ApprovalComment.class);
        verify(commentMapper).countByExample(criteria.capture());
        assertEquals("resource-1", criteria.getValue().getResourceId());
        assertEquals("org-1", criteria.getValue().getOrganizationId());
        assertNull(criteria.getValue().getParentId());
        if (!latestInstance) {
            verifyNoInteractions(instanceMapper);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingApprovalInstanceDoesNotCountComments() {
        ApprovalInstanceService service = new ApprovalInstanceService();
        BaseMapper<ApprovalComment> commentMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalCommentMapper", commentMapper);

        assertNull(service.getLatestApprovalInstanceDetail("resource-1", "org-1"));
        verifyNoInteractions(commentMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoApprovedNodeExposesStoredComment() {
        ApprovalInstanceService service = new ApprovalInstanceService();
        BaseMapper<ApprovalNodeApprover> approverNodeMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", approverNodeMapper);
        when(approverNodeMapper.selectByIds(anyList())).thenReturn(List.of());
        ApprovalRecord record = new ApprovalRecord();
        record.setId("record-auto");
        record.setNodeId("NODE001");
        record.setNodeRound(1);
        record.setResult(ApprovalStatus.AUTO_APPROVED.name());
        record.setComment("审批人为空，自动通过");
        record.setCreateTime(123L);

        List<ApprovalRecordNode> nodes = ReflectionTestUtils.invokeMethod(service, "buildProcessedApprovalNodes",
                new ApprovalInstance(),
                List.of(), List.of(record), List.of(), List.of(), Map.of(), Map.of(), "org-1");

        ApprovalTaskNode autoTask = nodes.getFirst().getTaskNodes().getFirst();
        assertEquals(record.getComment(), autoTask.getComment());
        assertEquals(record.getResult(), autoTask.getApprovalStatus());
        assertEquals(record.getCreateTime(), autoTask.getApprovalTime());
        assertEquals(record.getId(), autoTask.getRecordId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvedReturnedNodeDoesNotExposePreviousBackInfo() {
        ApprovalInstanceService service = new ApprovalInstanceService();
        ApprovalFlowService flowService = mock(ApprovalFlowService.class);
        BaseMapper<ApprovalNodeApprover> approverNodeMapper = mock(BaseMapper.class);
        BaseMapper<ApprovalNode> nodeMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", approverNodeMapper);
        ReflectionTestUtils.setField(service, "approvalNodeMapper", nodeMapper);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-1");
        instance.setCurrentNodeId("NODE001");
        instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
        ApprovalTask task = task("task-1", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 1);
        task.setInstanceId(instance.getId());
        task.setNodeId("NODE001");
        task.setCreateTime(1L);
        ApprovalRecord record = new ApprovalRecord();
        record.setId("record-1");
        record.setTaskId(task.getId());
        record.setNodeId(task.getNodeId());
        record.setNodeRound(task.getNodeRound());
        record.setCreateTime(2L);
        ApprovalReturnBackRecord backRecord = new ApprovalReturnBackRecord();
        backRecord.setId("back-1");
        backRecord.setReturnToNodeId(task.getNodeId());
        backRecord.setReturnReason("之前的退回原因");
        Attachment attachment = new Attachment();

        when(flowService.getCurrentNodeApproverList(instance, task.getNodeId(), "org-1"))
                .thenReturn(List.of(task.getApproverId()));
        when(approverNodeMapper.selectByIds(anyList())).thenReturn(List.of());
        when(nodeMapper.selectByIds(anyList())).thenReturn(List.of());
        when(nodeMapper.selectOne(any())).thenReturn(null);

        List<ApprovalRecordNode> nodes = ReflectionTestUtils.invokeMethod(service, "buildApprovalRecordNodeList",
                instance,
                List.of(task), List.of(record), List.of(), List.of(backRecord),
                Map.of(backRecord.getId(), List.of(attachment)), Map.of(), "org-1");

        ApprovalRecordNode node = nodes.getFirst();
        assertFalse(node.isBackNode());
        assertNull(node.getBackReason());
        assertEquals(List.of(), node.getBackAttachments());
    }

    @Test
    void approvingTaskDoesNotExposePreviousApprovalTime() {
        ApprovalTask task = task("task-1", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalRecord record = new ApprovalRecord();
        record.setId("record-1");
        record.setCreateTime(123L);

        ApprovalTaskNode result = ReflectionTestUtils.invokeMethod(new ApprovalInstanceService(), "buildTaskNode", task,
                Map.of(task.getId(), record), Map.of(), Map.of(), Map.of());

        assertNull(result.getApprovalTime());
        task.setStatus(ApprovalStatus.APPROVED.name());
        result = ReflectionTestUtils.invokeMethod(new ApprovalInstanceService(), "buildTaskNode", task,
                Map.of(task.getId(), record), Map.of(), Map.of(), Map.of());
        assertEquals(123L, result.getApprovalTime());
    }

    @Test
    void returnedSignChainKeepsOrderAndResetsFollowingTasks() {
        ApprovalTask root = task("root-new", ApprovalTaskType.NL, ApprovalStatus.PENDING, 2);
        ApprovalTask previous = task("sign-previous", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
        ApprovalTask current = task("sign-current", ApprovalTaskType.SN, ApprovalStatus.APPROVING, 2);
        ApprovalTask following = task("sign-following", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);

        List<ApprovalAddSignTask> signs = new ArrayList<>(List.of(
                sign("relation-previous", previous.getId(), root.getId(), root.getId(), ApprovalAddSignType.BEFORE,
                        100L),
                sign("relation-current", current.getId(), root.getId(), root.getId(), ApprovalAddSignType.BEFORE, 200L),
                sign("relation-following", following.getId(), root.getId(), root.getId(), ApprovalAddSignType.AFTER,
                        300L)));
        Map<String, ApprovalTask> signTaskMap = Map.of(
                previous.getId(), previous,
                current.getId(), current,
                following.getId(), following);

        @SuppressWarnings("unchecked")
        List<ApprovalTask> result = ReflectionTestUtils.invokeMethod(new ApprovalInstanceService(), "flatSignTask",
                root, Map.of(root.getId(), signs), signTaskMap);

        assertEquals(List.of("sign-previous", "sign-current", "root-new", "sign-following"),
                result.stream().map(ApprovalTask::getId).toList());
        assertEquals(ApprovalStatus.APPROVED.name(), result.get(0).getStatus());
        assertEquals(ApprovalStatus.APPROVING.name(), result.get(1).getStatus());
        assertEquals(ApprovalStatus.PENDING.name(), result.get(2).getStatus());
        assertEquals(ApprovalStatus.PENDING.name(), result.get(3).getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void futurePendingNodeReplacesApprovedHistoryFromPreviousReturnRound() {
        ApprovalInstanceService service = new ApprovalInstanceService();
        ApprovalFlowService flowService = mock(ApprovalFlowService.class);
        BaseMapper<ApprovalNodeApprover> approverNodeMapper = mock(BaseMapper.class);
        BaseMapper<ApprovalNode> nodeMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", approverNodeMapper);
        ReflectionTestUtils.setField(service, "approvalNodeMapper", nodeMapper);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-1");
        instance.setCurrentNodeId("NODE001");
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        ApprovalTask current = task("task-current", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 2);
        current.setNodeId("NODE001");
        current.setCreateTime(30L);
        ApprovalTask stale = task("task-stale", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 1);
        stale.setNodeId("NODE002");
        stale.setCreateTime(10L);
        ApprovalTask staleSign = task("sign-stale", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
        staleSign.setNodeId(stale.getNodeId());
        staleSign.setCreateTime(11L);
        ApprovalAddSignTask staleRelation = sign("relation-stale", staleSign.getId(), stale.getId(), stale.getId(),
                ApprovalAddSignType.AFTER, 100L);
        ApprovalRecord staleRecord = new ApprovalRecord();
        staleRecord.setId("record-stale");
        staleRecord.setTaskId(stale.getId());
        staleRecord.setNodeId(stale.getNodeId());
        staleRecord.setNodeRound(1);
        staleRecord.setCreateTime(20L);
        ApprovalNodeApproverResponse future = new ApprovalNodeApproverResponse();
        future.setId(stale.getNodeId());
        future.setApprovalType("MANUAL");
        future.setApproverList(List.of(stale.getApproverId()));
        ApprovalNode firstNode = new ApprovalNode();
        firstNode.setId(current.getNodeId());
        firstNode.setNodeType("APPROVER");
        firstNode.setSort(1);
        ApprovalNode secondNode = new ApprovalNode();
        secondNode.setId(stale.getNodeId());
        secondNode.setNodeType("APPROVER");
        secondNode.setSort(2);
        when(flowService.getCurrentNodeApproverList(instance, current.getNodeId(), "org-1"))
                .thenReturn(List.of(current.getApproverId()));
        when(flowService.getCurrentNodeApproverList(instance, stale.getNodeId(), "org-1"))
                .thenReturn(List.of(stale.getApproverId()));
        when(flowService.getInstanceCurrentFollowNode(instance, "org-1")).thenReturn(List.of(future));
        when(approverNodeMapper.selectByIds(anyList())).thenReturn(List.of());
        when(nodeMapper.selectByIds(anyList())).thenReturn(List.of(firstNode, secondNode));
        when(nodeMapper.selectOne(any())).thenReturn(null);

        ApplicationContext previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class,
                "context");
        DefaultUidGenerator generator = new DefaultUidGenerator();
        ReflectionTestUtils.setField(generator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
        generator.init();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(DefaultUidGenerator.class)).thenReturn(generator);
        new CommonBeanFactory().setApplicationContext(context);
        try {
            List<ApprovalRecordNode> nodes = ReflectionTestUtils.invokeMethod(service, "buildApprovalRecordNodeList",
                    instance,
                    List.of(current, stale, staleSign), List.of(staleRecord), List.of(staleRelation), List.of(),
                    Map.of(), Map.of(), "org-1");

            assertEquals(List.of("NODE001", "NODE002"), nodes.stream().map(ApprovalRecordNode::getNodeId).toList());
            assertEquals(ApprovalStatus.APPROVING.name(), nodes.getFirst().getApprovalStatus());
            ApprovalRecordNode pending = nodes.getLast();
            assertEquals(ApprovalStatus.PENDING.name(), pending.getApprovalStatus());
            assertEquals(ApprovalStatus.PENDING.name(), pending.getTaskNodes().getFirst().getApprovalStatus());
            assertNull(pending.getTaskNodes().getFirst().getApprovalTime());
        } finally {
            new CommonBeanFactory().setApplicationContext(previousContext);
        }
    }

    private ApprovalTask task(String id, ApprovalTaskType type, ApprovalStatus status, int round) {
        ApprovalTask task = new ApprovalTask();
        task.setId(id);
        task.setApproverId(id + "-approver");
        task.setType(type.name());
        task.setStatus(status.name());
        task.setNodeRound(round);
        return task;
    }

    private ApprovalAddSignTask sign(String id, String taskId, String signTaskId, String rootTaskId,
            ApprovalAddSignType type, long sort) {
        ApprovalAddSignTask sign = new ApprovalAddSignTask();
        sign.setId(id);
        sign.setTaskId(taskId);
        sign.setSignTaskId(signTaskId);
        sign.setRootTaskId(rootTaskId);
        sign.setType(type.name());
        sign.setSort(sort);
        return sign;
    }
}
