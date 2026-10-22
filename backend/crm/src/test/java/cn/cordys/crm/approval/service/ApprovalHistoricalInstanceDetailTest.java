package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.approval.dto.ApprovalTaskNode;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalHistoricalInstanceDetailTest {

    private ApprovalInstanceService service;
    private ApprovalInstance instance;
    private BaseMapper<ApprovalTask> taskMapper;
    private BaseMapper<ApprovalRecord> recordMapper;
    private BaseMapper<ApprovalAddSignTask> signMapper;
    private BaseMapper<ApprovalNode> nodeMapper;
    private BaseMapper<ApprovalNodeApprover> approverMapper;
    private BaseMapper<ApprovalInstanceAttachment> instanceAttachmentMapper;
    private BaseMapper<Attachment> attachmentMapper;
    private UserExtendService userExtendService;
    private ApprovalFlowService flowService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalInstanceService();
        taskMapper = mock(BaseMapper.class);
        recordMapper = mock(BaseMapper.class);
        signMapper = mock(BaseMapper.class);
        nodeMapper = mock(BaseMapper.class);
        approverMapper = mock(BaseMapper.class);
        instanceAttachmentMapper = mock(BaseMapper.class);
        attachmentMapper = mock(BaseMapper.class);
        userExtendService = mock(UserExtendService.class);
        flowService = mock(ApprovalFlowService.class);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "approvalRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "approvalAddSignTaskMapper", signMapper);
        ReflectionTestUtils.setField(service, "approvalNodeMapper", nodeMapper);
        ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", approverMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceAttachmentMapper", instanceAttachmentMapper);
        ReflectionTestUtils.setField(service, "attachmentMapper", attachmentMapper);
        ReflectionTestUtils.setField(service, "approvalReturnBackRecordMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "approvalCommentMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "userExtendService", userExtendService);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        when(userExtendService.getAllUserSimpleMap()).thenReturn(Map.of());
        instance = new ApprovalInstance();
        instance.setId("instance-old");
        instance.setFlowVersionId("version-old");
        instance.setCurrentNodeId("NODE002");
        instance.setSubmitterId("submitter");
        instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
    }

    @Test
    void rejectedHistoryKeepsOriginalNodeModeApproversOpinionsAndAttachments() {
        ApprovalTask first = task("first", "NODE001", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 10L);
        ApprovalTask approved = task("approved", "NODE002", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 20L);
        ApprovalTask rejected = task("rejected", "NODE002", ApprovalTaskType.NL, ApprovalStatus.UNAPPROVED, 21L);
        ApprovalTask future = task("future", "NODE003", ApprovalTaskType.NL, ApprovalStatus.PENDING, 30L);
        ApprovalRecord record = record(rejected, "请补齐合同信息", 25L);
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(first, approved, rejected, future));
        when(recordMapper.selectListByLambda(any())).thenReturn(List.of(record));
        when(nodeMapper.selectByIds(anyList())).thenReturn(List.of(
                node("NODE001", "旧版初审", "APPROVER", 1),
                node("NODE002", "旧版区域会签", "APPROVER", 2),
                node("NODE003", "旧版后续节点", "APPROVER", 3)));
        when(approverMapper.selectByIds(anyList())).thenReturn(List.of(
                approver("NODE001", MultiApproverModeEnum.ALL), approver("NODE002", MultiApproverModeEnum.ALL)));
        ApprovalInstanceAttachment relation = new ApprovalInstanceAttachment();
        relation.setInstanceId(instance.getId());
        relation.setElementId(record.getId());
        relation.setAttachmentId("attachment-old");
        Attachment attachment = new Attachment();
        attachment.setId(relation.getAttachmentId());
        when(instanceAttachmentMapper.selectListByLambda(any())).thenReturn(List.of(relation));
        when(attachmentMapper.selectByIds(List.of(attachment.getId()))).thenReturn(List.of(attachment));
        when(userExtendService.getUserOptionByIds(anyList())).thenReturn(List.of());

        ApprovalInstanceDetail detail = service.getHistoricalApprovalInstanceDetail(instance, "org-1");

        assertEquals(instance.getId(), detail.getId());
        assertEquals(List.of("NODE001", "NODE002"), detail.getNodes().stream()
                .map(ApprovalRecordNode::getNodeId).toList());
        ApprovalRecordNode rejectNode = detail.getNodes().getLast();
        assertEquals("旧版区域会签", rejectNode.getNodeName());
        assertEquals(MultiApproverModeEnum.ALL, rejectNode.getMultiApproverMode());
        assertEquals(ApprovalStatus.UNAPPROVED.name(), rejectNode.getApprovalStatus());
        assertEquals(List.of(approved.getApproverId(), rejected.getApproverId()), rejectNode.getTaskNodes().stream()
                .map(ApprovalTaskNode::getApproverId).toList());
        ApprovalTaskNode rejectedTask = rejectNode.getTaskNodes().getLast();
        assertEquals(record.getComment(), rejectedTask.getComment());
        assertEquals(record.getCreateTime(), rejectedTask.getApprovalTime());
        assertEquals(List.of(attachment), rejectedTask.getAttachments());
        assertFalse(detail.getNodes().stream().anyMatch(ApprovalRecordNode::isEndNode));
        verifyNoInteractions(flowService);
    }

    @Test
    void approvedHistoryIncludesCompletePathAndEndNodeFromOriginalVersion() {
        instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
        ApprovalTask first = task("first", "NODE001", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 10L);
        ApprovalTask last = task("last", "NODE002", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 20L);
        ApprovalNode end = node("END-OLD", "旧版审批结束", "END", 3);
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(first, last));
        when(nodeMapper.selectOne(any())).thenReturn(end);
        when(nodeMapper.selectByIds(anyList())).thenReturn(List.of(
                node("NODE001", "旧版初审", "APPROVER", 1),
                node("NODE002", "旧版复审", "APPROVER", 2), end));
        when(approverMapper.selectByIds(anyList())).thenReturn(List.of(
                approver("NODE001", MultiApproverModeEnum.ALL), approver("NODE002", MultiApproverModeEnum.ALL)));

        ApprovalInstanceDetail detail = service.getHistoricalApprovalInstanceDetail(instance, "org-1");

        assertEquals(List.of("NODE001", "NODE002", end.getId()), detail.getNodes().stream()
                .map(ApprovalRecordNode::getNodeId).toList());
        assertTrue(detail.getNodes().getLast().isEndNode());
        assertEquals(end.getName(), detail.getNodes().getLast().getNodeName());
        ArgumentCaptor<ApprovalNode> criteria = ArgumentCaptor.forClass(ApprovalNode.class);
        verify(nodeMapper).selectOne(criteria.capture());
        assertEquals(instance.getFlowVersionId(), criteria.getValue().getFlowVersionId());
        verifyNoInteractions(flowService);
    }

    @Test
    void sequentialHistoryOnlyContainsStoredTasksWithoutResolvingCurrentRoleMembers() {
        ApprovalTask first = task("first", "NODE002", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 10L);
        ApprovalTask rejected = task("rejected", "NODE002", ApprovalTaskType.NL, ApprovalStatus.UNAPPROVED, 20L);
        ApprovalNodeApprover approver = approver("NODE002", MultiApproverModeEnum.SEQUENTIAL);
        approver.setApproverType("ROLE");
        approver.setApproverList("[\"current-role\"]");
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(first, rejected));
        when(nodeMapper.selectByIds(anyList())).thenReturn(List.of(node("NODE002", "旧版依次审批", "APPROVER", 1)));
        when(approverMapper.selectByIds(anyList())).thenReturn(List.of(approver));

        ApprovalInstanceDetail detail = service.getHistoricalApprovalInstanceDetail(instance, "org-1");

        assertEquals(1, detail.getNodes().size());
        assertEquals(MultiApproverModeEnum.SEQUENTIAL, detail.getNodes().getFirst().getMultiApproverMode());
        assertEquals(List.of(first.getId(), rejected.getId()), detail.getNodes().getFirst().getTaskNodes().stream()
                .map(ApprovalTaskNode::getTaskId).toList());
        verifyNoInteractions(flowService);
    }

    @Test
    void rejectedBeforeSignStopsBeforeUnprocessedRootAndFollowingSigns() {
        instance.setCurrentNodeId("NODE001");
        ApprovalTask root = task("root", "NODE001", ApprovalTaskType.NL, ApprovalStatus.PENDING, 10L);
        ApprovalTask previous = task("previous", "NODE001-SN", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 20L);
        ApprovalTask rejected = task("rejected", "NODE001-SN", ApprovalTaskType.SN, ApprovalStatus.UNAPPROVED, 30L);
        ApprovalTask following = task("following", "NODE001-SN", ApprovalTaskType.SN, ApprovalStatus.PENDING, 40L);
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(root, previous, rejected, following));
        when(signMapper.selectListByLambda(any())).thenReturn(List.of(
                sign(previous, root, ApprovalAddSignType.BEFORE, 1L),
                sign(rejected, root, ApprovalAddSignType.BEFORE, 2L),
                sign(following, root, ApprovalAddSignType.AFTER, 3L)));
        when(nodeMapper.selectByIds(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            return ids.contains("NODE001") ? List.of(node("NODE001", "旧版单人审批", "APPROVER", 1)) : List.of();
        });
        when(approverMapper.selectByIds(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            return ids.contains("NODE001") ? List.of(approver("NODE001", MultiApproverModeEnum.ALL)) : List.of();
        });

        ApprovalInstanceDetail detail = service.getHistoricalApprovalInstanceDetail(instance, "org-1");

        assertEquals(List.of(previous.getId(), rejected.getId()), detail.getNodes().stream()
                .flatMap(node -> node.getTaskNodes().stream()).map(ApprovalTaskNode::getTaskId).toList());
        assertEquals(ApprovalStatus.UNAPPROVED.name(), detail.getNodes().getLast().getApprovalStatus());
        assertTrue(detail.getNodes().stream().allMatch(node -> "旧版单人审批".equals(node.getNodeName())));
        verifyNoInteractions(flowService);
    }

    @Test
    void latestDetailStillAppendsFutureAndEndNodes() {
        ApprovalTask rejected = task("rejected", "NODE002", ApprovalTaskType.NL, ApprovalStatus.UNAPPROVED, 10L);
        ApprovalNodeApproverResponse future = new ApprovalNodeApproverResponse();
        future.setId("NODE003");
        future.setApprovalType("AUTO_PASS");
        future.setApproverList(List.of());
        ApprovalNode end = node("END-OLD", "审批结束", "END", 4);
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(rejected));
        when(flowService.getCurrentNodeApproverList(instance, "NODE002", "org-1"))
                .thenReturn(List.of(rejected.getApproverId()));
        when(flowService.getInstanceCurrentFollowNode(instance, "org-1")).thenReturn(List.of(future));
        when(nodeMapper.selectOne(any())).thenReturn(end);
        when(nodeMapper.selectByIds(anyList())).thenReturn(List.of(
                node("NODE002", "当前审批", "APPROVER", 2), node("NODE003", "未来审批", "APPROVER", 3), end));
        when(approverMapper.selectByIds(anyList())).thenReturn(List.of(approver("NODE002", MultiApproverModeEnum.ALL)));

        ApprovalInstanceDetail detail = service.getApprovalInstanceDetail(instance, "org-1");

        assertEquals(List.of("NODE002", "NODE003", end.getId()), detail.getNodes().stream()
                .map(ApprovalRecordNode::getNodeId).toList());
        assertEquals(ApprovalStatus.PENDING.name(), detail.getNodes().get(1).getApprovalStatus());
        assertTrue(detail.getNodes().getLast().isEndNode());
        verify(flowService).getInstanceCurrentFollowNode(instance, "org-1");
    }

    private ApprovalTask task(String id, String nodeId, ApprovalTaskType type, ApprovalStatus status, long createTime) {
        ApprovalTask task = new ApprovalTask();
        task.setId(id);
        task.setInstanceId(instance.getId());
        task.setNodeId(nodeId);
        task.setApproverId(id + "-original-user");
        task.setType(type.name());
        task.setStatus(status.name());
        task.setNodeRound(1);
        task.setCreateTime(createTime);
        return task;
    }

    private ApprovalRecord record(ApprovalTask task, String comment, long createTime) {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(task.getId() + "-record");
        record.setInstanceId(instance.getId());
        record.setTaskId(task.getId());
        record.setNodeId(task.getNodeId());
        record.setNodeRound(task.getNodeRound());
        record.setResult(task.getStatus());
        record.setComment(comment);
        record.setCreateTime(createTime);
        return record;
    }

    private ApprovalNode node(String id, String name, String type, int sort) {
        ApprovalNode node = new ApprovalNode();
        node.setId(id);
        node.setFlowVersionId(instance.getFlowVersionId());
        node.setName(name);
        node.setNodeType(type);
        node.setSort(sort);
        return node;
    }

    private ApprovalNodeApprover approver(String id, MultiApproverModeEnum mode) {
        ApprovalNodeApprover approver = new ApprovalNodeApprover();
        approver.setId(id);
        approver.setFlowVersionId(instance.getFlowVersionId());
        approver.setMultiApproverMode(mode.name());
        return approver;
    }

    private ApprovalAddSignTask sign(ApprovalTask task, ApprovalTask root, ApprovalAddSignType type, long sort) {
        ApprovalAddSignTask sign = new ApprovalAddSignTask();
        sign.setId(task.getId() + "-sign");
        sign.setTaskId(task.getId());
        sign.setRootTaskId(root.getId());
        sign.setSignTaskId(root.getId());
        sign.setType(type.name());
        sign.setSort(sort);
        return sign;
    }
}
