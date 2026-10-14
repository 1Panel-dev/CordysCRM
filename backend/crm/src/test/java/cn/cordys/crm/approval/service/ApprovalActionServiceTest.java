package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.constants.ApprovalTypeEnum;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalActionRequest;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.common.constants.FormKey;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalActionServiceTest {

	private static ApplicationContext previousContext;
	private static MessageSource previousMessageSource;
	private static DefaultUidGenerator uidGenerator;

	private ApprovalActionService service;
	private BaseMapper<ApprovalInstance> instanceMapper;
	private BaseMapper<ApprovalTask> taskMapper;
	private BaseMapper<ApprovalAddSignTask> addSignTaskMapper;
	private BaseMapper<ApprovalNodeApprover> nodeApproverMapper;
	private ExtApprovalInstanceMapper extInstanceMapper;
	private ExtApprovalTaskMapper extTaskMapper;
	private ApprovalFlowService approvalFlowService;

	@BeforeAll
	static void initIdGenerator() {
		previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
		previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("no.back.auto.approval", LocaleContextHolder.getLocale(), "自动节点无法退回!");
		ReflectionTestUtils.setField(Translator.class, "messageSource", messageSource);
		DefaultUidGenerator generator = new DefaultUidGenerator();
		ReflectionTestUtils.setField(generator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
		generator.init();
		uidGenerator = generator;
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(DefaultUidGenerator.class)).thenReturn(generator);
		new CommonBeanFactory().setApplicationContext(context);
	}

	@AfterAll
	static void restoreContext() {
		new CommonBeanFactory().setApplicationContext(previousContext);
		ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
	}

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		service = new ApprovalActionService();
		instanceMapper = mock(BaseMapper.class);
		taskMapper = mock(BaseMapper.class);
		addSignTaskMapper = mock(BaseMapper.class);
		nodeApproverMapper = mock(BaseMapper.class);
		extInstanceMapper = mock(ExtApprovalInstanceMapper.class);
		extTaskMapper = mock(ExtApprovalTaskMapper.class);
		approvalFlowService = mock(ApprovalFlowService.class);
		ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
		ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
		ReflectionTestUtils.setField(service, "approvalAddSignTaskMapper", addSignTaskMapper);
		ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", nodeApproverMapper);
		ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", extInstanceMapper);
		ReflectionTestUtils.setField(service, "extApprovalTaskMapper", extTaskMapper);
		ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
	}

	@Test
	void backToAutomaticNodeReturnsFriendlyMessage() {
		ApprovalInstance instance = new ApprovalInstance();
		instance.setId("instance-1");
		when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);

		ApprovalNodeApprover nodeApprover = new ApprovalNodeApprover();
		when(nodeApproverMapper.selectByPrimaryKey("AUTO001")).thenReturn(nodeApprover);
		ApprovalReturnBackRequest request = new ApprovalReturnBackRequest();
		request.setInstanceId("instance-1");
		request.setReturnToNodeId("AUTO001");

		for (ApprovalTypeEnum type : List.of(ApprovalTypeEnum.AUTO_PASS, ApprovalTypeEnum.AUTO_REJECT)) {
			nodeApprover.setApprovalType(type.name());
			GenericException exception = assertThrows(GenericException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "appendBackTasks", request, "submitter", "org-1"));
			assertEquals("自动节点无法退回!", exception.getMessage());
		}
		verifyNoInteractions(approvalFlowService);
	}

	@Test
	void sequentialApprovalReentryIgnoresApprovedTasksFromExpiredRound() {
		ApprovalInstance instance = new ApprovalInstance();
		instance.setId("instance-1");
		when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);
		when(approvalFlowService.getCurrentNodeApproverList(instance, "NODE001", "org-1"))
				.thenReturn(List.of("approver-1", "approver-2"));
		when(extInstanceMapper.getNodeRound("instance-1", "NODE001")).thenReturn(0);

		ApprovalTask expiredTask = task("expired", ApprovalTaskType.NL, ApprovalStatus.APPROVED, -1);
		ApprovalTask currentTask = task("current", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 0);
		when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
			LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
			return wrapper.getParams().containsValue(0) ? List.of(currentTask) : List.of(expiredTask, currentTask);
		});

		assertEquals("approver-2", service.getMultiSeqAfterOne("NODE001", "instance-1", "org-1", 0));
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

	@Test
	@SuppressWarnings("unchecked")
	void rejectRevertsTheResourceBeforeApplyingTheRejectPostActions() {
		ApprovalInstance instance = new ApprovalInstance();
		instance.setId("instance-1");
		instance.setType(FormKey.CUSTOMER.getKey());
		instance.setResourceId("customer-1");
		instance.setExecuteTime(ExecuteTimingEnum.UPDATE.name());
		instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
		when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);

		ApprovalTask currentTask = task("task-1", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 0);
		currentTask.setApproverId("approver-1");
		when(taskMapper.selectByPrimaryKey("task-1")).thenReturn(currentTask);

		ReflectionTestUtils.setField(service, "approvalInstanceService", mock(ApprovalInstanceService.class));
		ReflectionTestUtils.setField(service, "approvalRecordMapper", mock(BaseMapper.class));

		ApprovalActionService spy = spy(service);
		doNothing().when(spy).loseCurrentNode(anyString(), anyString());
		doNothing().when(spy).saveLogAndNotice(any(), anyString(), anyString(), any());
		doNothing().when(spy).revertFromSnapshot(any(), any(), any(), any(), any());

		ApprovalResourceService resourceService = mock(ApprovalResourceService.class);
		try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
			beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class)).thenReturn(resourceService);
			beanFactory.when(() -> CommonBeanFactory.getBean(DefaultUidGenerator.class)).thenReturn(uidGenerator);
			spy.reject(ApprovalActionRequest.builder().id("task-1").instanceId("instance-1").nodeId("NODE001").build(),
					"approver-1", "org-1");
		}

		// 先退回再执行驳回后操作: 顺序反过来的话, 驳回后操作刚写进去的字段会被退回一并还原掉
		InOrder inOrder = inOrder(spy, approvalFlowService);
		inOrder.verify(spy).revertFromSnapshot(
				FormKey.CUSTOMER, ExecuteTimingEnum.UPDATE.name(), "customer-1", "approver-1", "org-1");
		inOrder.verify(approvalFlowService).updateApprovalPostField(
				instance, "NODE001", ApprovalAction.REJECT, "approver-1");
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
