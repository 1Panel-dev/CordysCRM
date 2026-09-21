package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalReturnBackRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.approval.dto.ApprovalTaskNode;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.when;

class ApprovalInstanceServiceTest {

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

		when(flowService.getCurrentNodeApproverList(instance, task.getNodeId(), "org-1")).thenReturn(List.of(task.getApproverId()));
		when(approverNodeMapper.selectByIds(anyList())).thenReturn(List.of());
		when(nodeMapper.selectByIds(anyList())).thenReturn(List.of());
		when(nodeMapper.selectOne(any())).thenReturn(null);

		List<ApprovalRecordNode> nodes = ReflectionTestUtils.invokeMethod(service, "buildApprovalRecordNodeList", instance,
				List.of(task), List.of(record), List.of(), List.of(backRecord), Map.of(backRecord.getId(), List.of(attachment)), Map.of(), "org-1");

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
				sign("relation-previous", previous.getId(), root.getId(), root.getId(), ApprovalAddSignType.BEFORE, 100L),
				sign("relation-current", current.getId(), root.getId(), root.getId(), ApprovalAddSignType.BEFORE, 200L),
				sign("relation-following", following.getId(), root.getId(), root.getId(), ApprovalAddSignType.AFTER, 300L)
		));
		Map<String, ApprovalTask> signTaskMap = Map.of(
				previous.getId(), previous,
				current.getId(), current,
				following.getId(), following
		);

		@SuppressWarnings("unchecked")
		List<ApprovalTask> result = ReflectionTestUtils.invokeMethod(new ApprovalInstanceService(), "flatSignTask", root, Map.of(root.getId(), signs), signTaskMap);

		assertEquals(List.of("sign-previous", "sign-current", "root-new", "sign-following"), result.stream().map(ApprovalTask::getId).toList());
		assertEquals(ApprovalStatus.APPROVED.name(), result.get(0).getStatus());
		assertEquals(ApprovalStatus.APPROVING.name(), result.get(1).getStatus());
		assertEquals(ApprovalStatus.PENDING.name(), result.get(2).getStatus());
		assertEquals(ApprovalStatus.PENDING.name(), result.get(3).getStatus());
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

	private ApprovalAddSignTask sign(String id, String taskId, String signTaskId, String rootTaskId, ApprovalAddSignType type, long sort) {
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
