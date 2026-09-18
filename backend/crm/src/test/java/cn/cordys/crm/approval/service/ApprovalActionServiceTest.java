package cn.cordys.crm.approval.service;

import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalActionServiceTest {

	private static ApplicationContext previousContext;

	private ApprovalActionService service;
	private BaseMapper<ApprovalInstance> instanceMapper;
	private BaseMapper<ApprovalTask> taskMapper;
	private BaseMapper<ApprovalAddSignTask> addSignTaskMapper;
	private ExtApprovalInstanceMapper extInstanceMapper;
	private ExtApprovalTaskMapper extTaskMapper;

	@BeforeAll
	static void initIdGenerator() {
		previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
		DefaultUidGenerator generator = new DefaultUidGenerator();
		ReflectionTestUtils.setField(generator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
		generator.init();
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(DefaultUidGenerator.class)).thenReturn(generator);
		new CommonBeanFactory().setApplicationContext(context);
	}

	@AfterAll
	static void restoreContext() {
		new CommonBeanFactory().setApplicationContext(previousContext);
	}

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		service = new ApprovalActionService();
		instanceMapper = mock(BaseMapper.class);
		taskMapper = mock(BaseMapper.class);
		addSignTaskMapper = mock(BaseMapper.class);
		extInstanceMapper = mock(ExtApprovalInstanceMapper.class);
		extTaskMapper = mock(ExtApprovalTaskMapper.class);
		ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
		ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
		ReflectionTestUtils.setField(service, "approvalAddSignTaskMapper", addSignTaskMapper);
		ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", extInstanceMapper);
		ReflectionTestUtils.setField(service, "extApprovalTaskMapper", extTaskMapper);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void backToSignTaskCreatesNewRoundFromRealNode() {
		ApprovalInstance instance = new ApprovalInstance();
		instance.setId("instance-1");
		when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);

		ApprovalTask oldRootTask = task("root-old", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
		ApprovalTask oldSignTask = task("sign-old", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
		when(taskMapper.selectByPrimaryKey("sign-old")).thenReturn(oldSignTask);
		when(taskMapper.selectByPrimaryKey("root-old")).thenReturn(oldRootTask);

		ApprovalAddSignTask targetSign = sign("sign-relation", "sign-old", "root-old", "root-old", 100L);
		when(addSignTaskMapper.selectOne(any())).thenReturn(targetSign);
		when(addSignTaskMapper.selectListByLambda(any())).thenReturn(List.of(targetSign));
		when(extInstanceMapper.getNextNodeRound("instance-1", "NODE001")).thenReturn(2);

		ApprovalReturnBackRequest request = new ApprovalReturnBackRequest();
		request.setInstanceId("instance-1");
		request.setReturnToNodeId("NODE001-SN0");
		request.setReturnToTaskId("sign-old");
		assertEquals("NODE001", request.getReturnToFlowNodeId());
		ReflectionTestUtils.invokeMethod(service, "appendBackTasks", request, "submitter", "org-1");

		ArgumentCaptor<List<ApprovalTask>> taskCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(taskMapper).batchInsert(taskCaptor.capture());
		List<ApprovalTask> tasks = taskCaptor.getValue();
		ApprovalTask newRootTask = tasks.get(0);
		ApprovalTask newSignTask = tasks.get(1);
		assertEquals(ApprovalStatus.PENDING.name(), newRootTask.getStatus());
		assertEquals(ApprovalStatus.APPROVING.name(), newSignTask.getStatus());
		assertEquals(2, newRootTask.getNodeRound());
		assertEquals(2, newSignTask.getNodeRound());
		assertNotEquals("root-old", newRootTask.getId());
		assertNotEquals("sign-old", newSignTask.getId());
		assertEquals(newSignTask.getId(), targetSign.getTaskId());
		verify(extTaskMapper).moveAddSignRoot("root-old", newRootTask.getId());
		verify(extTaskMapper).updateRootNext("root-old", newRootTask.getId());
		verify(extTaskMapper).updateRootNext("sign-old", newSignTask.getId());
	}

	@Test
	void nextSignTaskKeepsReturnedRound() {
		ApprovalTask currentTask = task("sign-current", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 2);
		ApprovalTask oldNextTask = task("sign-next-old", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
		when(taskMapper.selectByPrimaryKey("sign-current")).thenReturn(currentTask);
		when(taskMapper.selectByPrimaryKey("sign-next-old")).thenReturn(oldNextTask);

		ApprovalAddSignTask currentSign = sign("relation-current", "sign-current", "root-new", "root-new", 100L);
		ApprovalAddSignTask nextSign = sign("relation-next", "sign-next-old", "sign-current", "root-new", 200L);
		when(addSignTaskMapper.selectOne(any())).thenReturn(currentSign);
		when(addSignTaskMapper.selectListByLambda(any())).thenReturn(List.of(nextSign));

		ApprovalTask nextTask = ReflectionTestUtils.invokeMethod(service, "getNextAddSignTask", "sign-current");

		assertEquals(2, nextTask.getNodeRound());
		assertEquals(ApprovalStatus.APPROVING.name(), nextTask.getStatus());
		assertEquals(nextTask.getId(), nextSign.getTaskId());
		verify(extTaskMapper).updateRootNext("sign-next-old", nextTask.getId());
	}

	private ApprovalTask task(String id, ApprovalTaskType type, ApprovalStatus status, int round) {
		ApprovalTask task = new ApprovalTask();
		task.setId(id);
		task.setInstanceId("instance-1");
		task.setNodeId("NODE001");
		task.setApproverId(id + "-approver");
		task.setType(type.name());
		task.setStatus(status.name());
		task.setNodeRound(round);
		task.setCreateTime(1L);
		task.setUpdateTime(1L);
		return task;
	}

	private ApprovalAddSignTask sign(String id, String taskId, String signTaskId, String rootTaskId, long sort) {
		ApprovalAddSignTask sign = new ApprovalAddSignTask();
		sign.setId(id);
		sign.setTaskId(taskId);
		sign.setSignTaskId(signTaskId);
		sign.setRootTaskId(rootTaskId);
		sign.setType(ApprovalAddSignType.BEFORE.name());
		sign.setSort(sort);
		return sign;
	}
}
