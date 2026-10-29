package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
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
import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalActionBatchRequest;
import cn.cordys.crm.approval.dto.request.ApprovalActionRequest;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
        messageSource.addMessage("no.revoke.approval", LocaleContextHolder.getLocale(), "无法撤销");
        messageSource.addMessage("no.revoke.approval.next.sign", LocaleContextHolder.getLocale(), "后续为加签节点，无法撤销");
        messageSource.addMessage("approval.task.already.processed", LocaleContextHolder.getLocale(),
                "审批任务已处理或已失效，请刷新后重试");
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

    @ParameterizedTest
    @ValueSource(strings = { "APPROVED", "AUTO_APPROVED", "UNAPPROVED", "AUTO_UNAPPROVED", "NONE", "PENDING", "REVOKED" })
    void approveRejectsTasksThatAreNotPending(String status) {
        ApprovalTask task = task("task-1", ApprovalTaskType.NL, ApprovalStatus.valueOf(status), 1);
        prepareApproval(task, false);

        GenericException exception = assertThrows(GenericException.class,
                () -> service.approve(ApprovalActionRequest.builder().id(task.getId()).build(),
                        task.getApproverId(), "org-1"));

        assertEquals("审批任务已处理或已失效，请刷新后重试", exception.getMessage());
        assertEquals(status, task.getStatus());
        verifyNoInteractions(extTaskMapper, instanceMapper, approvalFlowService);
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void concurrentApprovalLosingTheConditionalUpdateDoesNotSaveRecordsOrAdvance() {
        ApprovalTask task = task("task-1", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        prepareApproval(task, false);
        when(extTaskMapper.approvePendingTask(any())).thenReturn(0);
        BaseMapper<ApprovalRecord> records = mock(BaseMapper.class);
        AttachmentService attachments = mock(AttachmentService.class);
        ReflectionTestUtils.setField(service, "approvalRecordMapper", records);
        ReflectionTestUtils.setField(service, "attachmentService", attachments);

        assertThrows(GenericException.class,
                () -> service.approve(ApprovalActionRequest.builder().id(task.getId())
                                .comment("同意").attachmentIds(List.of("attachment-1")).build(),
                        task.getApproverId(), "org-1"));

        verify(extTaskMapper).approvePendingTask(task);
        verifyNoInteractions(records, attachments, instanceMapper, approvalFlowService);
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void repeatedApprovalDoesNotCreateAnotherRoundOfTheNextNode() {
        ApprovalTask task = task("legal-task", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalInstance instance = prepareApproval(task, true);
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(task));
        when(extTaskMapper.approvePendingTask(any())).thenReturn(1);
        BaseMapper<ApprovalRecord> records = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalRecordMapper", records);
        ApprovalNodeApproverResponse sales = new ApprovalNodeApproverResponse();
        sales.setId("SALES001");
        sales.setNodeType("APPROVER");
        sales.setApproverList(List.of("sales-approver"));
        sales.setAutoSkipUserIds(List.of());
        sales.setSameSubmitterAction("SKIP");
        sales.setMultiApproverMode("ALL");
        when(approvalFlowService.getTaskNextNode(any(), any(), anyString())).thenReturn(sales);
        when(extInstanceMapper.getNextNodeRound(instance.getId(), sales.getId())).thenReturn(1);
        ApprovalActionRequest request = ApprovalActionRequest.builder().id(task.getId()).build();

        service.approve(request, task.getApproverId(), "org-1");
        assertThrows(GenericException.class, () -> service.approve(request, task.getApproverId(), "org-1"));

        ArgumentCaptor<List<ApprovalTask>> nextTasks = ArgumentCaptor.forClass((Class) List.class);
        verify(taskMapper).batchInsert(nextTasks.capture());
        assertEquals(1, nextTasks.getValue().size());
        assertEquals(1, nextTasks.getValue().getFirst().getNodeRound());
        assertEquals(ApprovalStatus.APPROVING.name(), nextTasks.getValue().getFirst().getStatus());
        assertEquals("SALES001", instance.getCurrentNodeId());
        verify(extTaskMapper).approvePendingTask(task);
        verify(records).insert(any());
        verify(approvalFlowService).getTaskNextNode(task, instance, "org-1");
        verify(approvalFlowService).updateApprovalPostField(instance, "NODE001", ApprovalAction.APPROVE,
                task.getApproverId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void otherApproversNewRoundsAndSignTasksCanStillApprove() {
        BaseMapper<ApprovalRecord> records = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalRecordMapper", records);
        when(extTaskMapper.approvePendingTask(any())).thenReturn(1);
        List<ApprovalTask> tasks = List.of(
                task("first-approver", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1),
                task("second-approver", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1),
                task("returned-task", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 2),
                task("sign-task", ApprovalTaskType.SN, ApprovalStatus.APPROVING, 2));

        for (ApprovalTask task : tasks) {
            when(taskMapper.selectByPrimaryKey(task.getId())).thenReturn(task);
            ReflectionTestUtils.invokeMethod(service, "saveActionTask",
                    ApprovalActionRequest.builder().id(task.getId()).build(), ApprovalAction.APPROVE,
                    task.getApproverId(), "org-1", null);
            assertEquals(ApprovalStatus.APPROVED.name(), task.getStatus());
            verify(extTaskMapper).approvePendingTask(task);
        }

        verify(records, times(tasks.size())).insert(any());
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    void approveAndBatchApproveKeepOneReadCommittedTransaction() throws Exception {
        AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();
        for (var method : List.of(
                ApprovalActionService.class.getMethod("approve", ApprovalActionRequest.class, String.class, String.class),
                ApprovalActionService.class.getMethod("batchApprove", ApprovalActionBatchRequest.class,
                        String.class, String.class))) {
            var transaction = attributes.getTransactionAttribute(method, ApprovalActionService.class);
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, transaction.getIsolationLevel());
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, transaction.getPropagationBehavior());
            assertTrue(transaction.rollbackOn(new Exception()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "FINISHED_INSTANCE", "OTHER_NODE", "OLD_ROUND", "CLEARED_ROUND", "OLD_SIGN_NODE" })
    void approveRejectsTasksOutsideTheCurrentNodeOrRound(String scenario) {
        ApprovalTask task = task("task-1", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalInstance instance = prepareApproval(task, false);
        switch (scenario) {
        case "FINISHED_INSTANCE" -> instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
        case "OTHER_NODE" -> instance.setCurrentNodeId("NEXT001");
        case "OLD_ROUND" -> when(extInstanceMapper.getNodeRound(instance.getId(), task.getNodeId())).thenReturn(2);
        case "CLEARED_ROUND" -> task.setNodeRound(-1);
        case "OLD_SIGN_NODE" -> {
            task.setType(ApprovalTaskType.SN.name());
            instance.setCurrentNodeId("NEXT001");
        }
        default -> throw new AssertionError(scenario);
        }

        assertThrows(GenericException.class,
                () -> service.approve(ApprovalActionRequest.builder().id(task.getId()).build(),
                        task.getApproverId(), "org-1"));

        assertEquals(ApprovalStatus.APPROVING.name(), task.getStatus());
        verifyNoInteractions(extTaskMapper, instanceMapper, approvalFlowService);
        verify(taskMapper, never()).updateById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "NL", "SN" })
    @SuppressWarnings("unchecked")
    void approveLocksTheInstanceBeforeCompletingAValidTask(String type) {
        ApprovalTask task = task("task-1", ApprovalTaskType.valueOf(type), ApprovalStatus.APPROVING, 2);
        ApprovalInstance instance = prepareApproval(task, false);
        BaseMapper<ApprovalRecord> records = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalRecordMapper", records);
        when(extTaskMapper.approvePendingTask(any())).thenReturn(1);

        service.approve(ApprovalActionRequest.builder().id(task.getId()).build(), task.getApproverId(), "org-1");

        InOrder order = inOrder(extInstanceMapper, taskMapper, extTaskMapper);
        order.verify(extInstanceMapper).selectByTaskIdForUpdate(task.getId());
        order.verify(taskMapper, times(2)).selectByPrimaryKey(task.getId());
        order.verify(extTaskMapper).approvePendingTask(task);
        assertEquals(ApprovalStatus.APPROVED.name(), task.getStatus());
        assertEquals("END001", instance.getCurrentNodeId());
    }

    @ParameterizedTest
    @ValueSource(strings = { "SINGLE", "ALL", "SEQUENTIAL" })
    void expiredRoundDoesNotBlockTheCurrentNodeCompletion(String mode) {
        ApprovalInstance instance = prepareAnyNode(!"SINGLE".equals(mode));
        ApprovalNodeApprover node = new ApprovalNodeApprover();
        node.setMultiApproverMode("SINGLE".equals(mode) ? "ANY" : mode);
        node.setSameSubmitterAction("ALLOW");
        when(nodeApproverMapper.selectByPrimaryKey("NODE001")).thenReturn(node);
        ApprovalTask expired = task("old-task", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalTask current = task("current-task", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 2);
        mockTaskQuery(List.of(expired, current));
        when(extInstanceMapper.getNodeRound(instance.getId(), "NODE001")).thenReturn(2);
        when(approvalFlowService.getCurrentNodeApproverList(instance, "NODE001", "org-1"))
                .thenReturn(List.of(current.getApproverId()));

        ReflectionTestUtils.invokeMethod(service, "approvedProcess", instance, current, current.getApproverId(),
                "org-1", "示例客户");

        assertEquals("END001", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.APPROVED.name(), instance.getApprovalStatus());
        assertEquals(ApprovalStatus.APPROVING.name(), expired.getStatus());
    }

    @Test
    void batchApproveLocksInstancesInAStableOrder() {
        ApprovalTask first = task("task-a", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalTask second = task("task-b", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        second.setInstanceId("instance-2");
        ApprovalActionBatchRequest request = new ApprovalActionBatchRequest();
        request.setIds(List.of(second.getId(), first.getId()));
        when(taskMapper.selectByIds(request.getIds())).thenReturn(List.of(second, first));
        ApprovalActionService spy = spy(service);
        doNothing().when(spy).approve(any(), anyString(), anyString());

        spy.batchApprove(request, "user-1", "org-1");

        ArgumentCaptor<ApprovalActionRequest> actions = ArgumentCaptor.forClass(ApprovalActionRequest.class);
        verify(spy, times(2)).approve(actions.capture(), anyString(), anyString());
        assertEquals(List.of(first.getId(), second.getId()),
                actions.getAllValues().stream().map(ApprovalActionRequest::getId).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void attachmentTransferFailureDoesNotSaveApprovalAttachmentRelation() {
        AttachmentService attachments = mock(AttachmentService.class);
        BaseMapper<ApprovalInstanceAttachment> relationMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "attachmentService", attachments);
        ReflectionTestUtils.setField(service, "approvalInstanceAttachmentMapper", relationMapper);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setResourceId("resource-1");
        when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);
        GenericException failure = new GenericException("附件转存失败");
        doThrow(failure).when(attachments).appendTemp(any());

        GenericException exception = assertThrows(GenericException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "saveInstanceAttachment", List.of("attachment-1"),
                        "instance-1", "element-1", "user-1", "org-1"));

        assertSame(failure, exception);
        verifyNoInteractions(relationMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvalAttachmentRelationsAreSavedOnlyAfterTransferCompletes() {
        AttachmentService attachments = mock(AttachmentService.class);
        BaseMapper<ApprovalInstanceAttachment> relationMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "attachmentService", attachments);
        ReflectionTestUtils.setField(service, "approvalInstanceAttachmentMapper", relationMapper);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setResourceId("resource-1");
        when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);
        List<String> attachmentIds = List.of("attachment-1", "attachment-2");
        List<String> savedIds = List.of("saved-1", "saved-2");
        when(attachments.appendTemp(any())).thenReturn(savedIds);

        ReflectionTestUtils.invokeMethod(service, "saveInstanceAttachment", attachmentIds,
                "instance-1", "element-1", "user-1", "org-1");

        InOrder order = inOrder(attachments, relationMapper);
        ArgumentCaptor<UploadTransferRequest> transfer = ArgumentCaptor.forClass(UploadTransferRequest.class);
        order.verify(attachments).appendTemp(transfer.capture());
        assertEquals("org-1", transfer.getValue().getOrganizationId());
        assertEquals("resource-1", transfer.getValue().getResourceId());
        assertEquals("user-1", transfer.getValue().getOperatorUserId());
        assertEquals(attachmentIds, transfer.getValue().getTempFileIds());
        ArgumentCaptor<ApprovalInstanceAttachment> relation = ArgumentCaptor.forClass(ApprovalInstanceAttachment.class);
        order.verify(relationMapper, times(2)).insert(relation.capture());
        assertEquals(savedIds, relation.getAllValues().stream()
                .map(ApprovalInstanceAttachment::getAttachmentId).toList());
        relation.getAllValues().forEach(attachment -> {
            assertEquals("instance-1", attachment.getInstanceId());
            assertEquals("element-1", attachment.getElementId());
        });
        order.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @SuppressWarnings("unchecked")
    void batchActionsReuseTemporaryInputsAndBindResourceCopies(boolean approve) {
        AttachmentService attachments = mock(AttachmentService.class);
        BaseMapper<ApprovalInstanceAttachment> relationMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "attachmentService", attachments);
        ReflectionTestUtils.setField(service, "approvalInstanceAttachmentMapper", relationMapper);
        ApprovalTask first = task("task-1", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 0);
        ApprovalTask second = task("task-2", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 0);
        second.setInstanceId("instance-2");
        when(taskMapper.selectByIds(List.of("task-1", "task-2"))).thenReturn(List.of(first, second));
        when(instanceMapper.selectByPrimaryKey(anyString())).thenAnswer(invocation -> {
            ApprovalInstance instance = new ApprovalInstance();
            instance.setResourceId("resource-" + invocation.<String>getArgument(0));
            return instance;
        });
        when(attachments.appendTemp(any())).thenAnswer(invocation -> {
            UploadTransferRequest transfer = invocation.getArgument(0);
            assertEquals(List.of("temporary-file"), transfer.getTempFileIds());
            return List.of("saved-" + transfer.getResourceId());
        });
        ApprovalActionService spy = spy(service);
        Answer<Void> saveAttachments = invocation -> {
            ApprovalActionRequest action = invocation.getArgument(0);
            ReflectionTestUtils.invokeMethod(spy, "saveInstanceAttachment", action.getAttachmentIds(),
                    action.getInstanceId(), action.getId(), "user-1", "org-1");
            return null;
        };
        ApprovalActionBatchRequest request = new ApprovalActionBatchRequest();
        request.setIds(List.of("task-1", "task-2"));
        request.setAttachmentIds(List.of("temporary-file"));

        if (approve) {
            doAnswer(saveAttachments).when(spy).approve(any(), anyString(), anyString());
            spy.batchApprove(request, "user-1", "org-1");
        } else {
            doAnswer(saveAttachments).when(spy).reject(any(), anyString(), anyString());
            spy.batchReject(request, "user-1", "org-1");
        }

        ArgumentCaptor<ApprovalInstanceAttachment> relations = ArgumentCaptor
                .forClass(ApprovalInstanceAttachment.class);
        verify(relationMapper, times(2)).insert(relations.capture());
        assertEquals(List.of("instance-1", "instance-2"), relations.getAllValues().stream()
                .map(ApprovalInstanceAttachment::getInstanceId).toList());
        assertEquals(List.of("saved-resource-instance-1", "saved-resource-instance-2"), relations.getAllValues().stream()
                .map(ApprovalInstanceAttachment::getAttachmentId).toList());
        assertEquals(List.of("task-1", "task-2"), relations.getAllValues().stream()
                .map(ApprovalInstanceAttachment::getElementId).toList());
        assertEquals(List.of("temporary-file"), request.getAttachmentIds());
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class,
                withSettings().mockMaker(MockMakers.INLINE))) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(resourceService);
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

    @Test
    void beforeSignOnSingleAnyNodeRestoresOriginalApproverWithoutAdvancing() {
        ApprovalInstance instance = prepareAnyNode(false);
        ApprovalTask rootTask = task("root-pending", ApprovalTaskType.NL, ApprovalStatus.PENDING, 1);
        ApprovalTask signTask = task("sign-approved", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
        when(taskMapper.selectByPrimaryKey(rootTask.getId())).thenReturn(rootTask);
        when(taskMapper.selectByPrimaryKey(signTask.getId())).thenReturn(signTask);
        when(addSignTaskMapper.selectOne(any())).thenReturn(
                sign("before-sign", signTask.getId(), rootTask.getId(), rootTask.getId(), 100L));
        when(addSignTaskMapper.selectListByLambda(any())).thenReturn(List.of());

        // 保留真实恢复后的待办，并模拟原节点清理会把它置为 NONE 的数据库行为。
        List<ApprovalTask> persisted = new ArrayList<>(List.of(rootTask, signTask));
        when(taskMapper.insert(any(ApprovalTask.class))).thenAnswer(invocation -> {
            persisted.add(invocation.getArgument(0));
            return 1;
        });
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            Object status = wrapper.getParams().get("status_eq");
            return persisted.stream().filter(task -> status == null || status.toString().equals(task.getStatus()))
                    .toList();
        });
        doAnswer(invocation -> {
            persisted.stream().filter(task -> ApprovalStatus.APPROVING.name().equals(task.getStatus()))
                    .forEach(task -> task.setStatus(ApprovalStatus.NONE.name()));
            return null;
        }).when(extInstanceMapper).loseApprovingTask("instance-1", "NODE001", 1);

        ReflectionTestUtils.invokeMethod(service, "approvedProcess", instance, signTask,
                signTask.getApproverId(), "org-1", "示例客户");

        ArgumentCaptor<ApprovalTask> restoredCaptor = ArgumentCaptor.forClass(ApprovalTask.class);
        verify(taskMapper).insert(restoredCaptor.capture());
        ApprovalTask restored = restoredCaptor.getValue();
        assertNotEquals(rootTask.getId(), restored.getId());
        assertEquals(rootTask.getApproverId(), restored.getApproverId());
        assertEquals(ApprovalTaskType.NL.name(), restored.getType());
        assertEquals(ApprovalStatus.APPROVING.name(), restored.getStatus());
        assertEquals("NODE001", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
        verify(approvalFlowService, never()).getTaskNextNode(any(), any(), anyString());
        verify(approvalFlowService, never()).updateApprovalPostField(any(), anyString(), any(), anyString());
        verify(extInstanceMapper, never()).loseApprovingTask(anyString(), anyString(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void completedOriginalTaskStillAdvancesSingleAndMultiAnyNodes(boolean multiApprover) {
        ApprovalInstance instance = prepareAnyNode(multiApprover);
        ApprovalTask currentTask = task("root-approved", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 1);
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            return wrapper.getParams().containsKey("status_eq") ? List.of() : List.of(currentTask);
        });

        ReflectionTestUtils.invokeMethod(service, "approvedProcess", instance, currentTask,
                currentTask.getApproverId(), "org-1", "示例客户");

        assertEquals("END001", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.APPROVED.name(), instance.getApprovalStatus());
        verify(approvalFlowService).getTaskNextNode(currentTask, instance, "org-1");
        verify(approvalFlowService).updateApprovalPostField(instance, "NODE001", ApprovalAction.APPROVE,
                currentTask.getApproverId());
        verify(extInstanceMapper).loseApprovingTask("instance-1", "NODE001", 1);
    }

    @Test
    void revokeBeforeSignSuspendsOriginalTaskAndReapprovalRestoresOnlyOnePendingTask() {
        ApprovalInstance instance = prepareAnyNode(false);
        ApprovalTask rootTask = task("root-restored", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalTask signTask = task("sign-approved", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
        signTask.setAction(ApprovalAction.APPROVE.name());
        when(taskMapper.selectByPrimaryKey(rootTask.getId())).thenReturn(rootTask);
        when(taskMapper.selectByPrimaryKey(signTask.getId())).thenReturn(signTask);
        ApprovalAddSignTask sign = sign("before-sign", signTask.getId(), rootTask.getId(), rootTask.getId(), 100L);
        when(addSignTaskMapper.selectOne(any())).thenReturn(sign);
        when(addSignTaskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalAddSignTask> wrapper = invocation.getArgument(0);
            return wrapper.getParams().containsKey("sort_gt") ? List.of() : List.of(sign);
        });
        List<ApprovalTask> persisted = new ArrayList<>(List.of(rootTask, signTask));
        mockTaskQuery(persisted);
        when(taskMapper.insert(any(ApprovalTask.class))).thenAnswer(invocation -> {
            persisted.add(invocation.getArgument(0));
            return 1;
        });

        ReflectionTestUtils.invokeMethod(service, "revokeProcess", signTask, instance, "org-1");
        assertEquals(ApprovalStatus.PENDING.name(), rootTask.getStatus());
        verify(taskMapper).updateById(rootTask);
        ReflectionTestUtils.invokeMethod(service, "refreshRevokeTask", signTask, instance, signTask.getApproverId());
        assertEquals(ApprovalStatus.APPROVING.name(), signTask.getStatus());
        assertNull(signTask.getAction());
        assertEquals("NODE001", instance.getCurrentNodeId());

        signTask.setStatus(ApprovalStatus.APPROVED.name());
        ReflectionTestUtils.invokeMethod(service, "approvedProcess", instance, signTask,
                signTask.getApproverId(), "org-1", "示例客户");

        ArgumentCaptor<ApprovalTask> restoredCaptor = ArgumentCaptor.forClass(ApprovalTask.class);
        verify(taskMapper).insert(restoredCaptor.capture());
        ApprovalTask restored = restoredCaptor.getValue();
        assertNotEquals(rootTask.getId(), restored.getId());
        assertEquals(rootTask.getApproverId(), restored.getApproverId());
        assertEquals(ApprovalStatus.APPROVING.name(), restored.getStatus());
        assertEquals(1,
                persisted.stream().filter(task -> ApprovalStatus.APPROVING.name().equals(task.getStatus())).count());
        verify(approvalFlowService, never()).getTaskNextNode(any(), any(), anyString());
        verify(extInstanceMapper, never()).batchClearApprovingTask(anyString(), anyString(), anyInt());
        verify(extInstanceMapper, never()).loseApprovingTask(anyString(), anyString(), anyInt());
    }

    @ParameterizedTest
    @CsvSource({ "false, AFTER", "true, AFTER", "true, BEFORE" })
    void revokeBeforeActiveSignReturnsSpecificMessageWithoutChangingTasks(boolean currentIsSign,
            ApprovalAddSignType nextSignType) {
        ApprovalInstance instance = prepareAnyNode(false);
        ApprovalTask rootTask = task("root", ApprovalTaskType.NL,
                currentIsSign ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED, 1);
        ApprovalTask currentTask = currentIsSign
                ? task("sign-current", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1)
                : rootTask;
        ApprovalTask nextTask = task("sign-next", ApprovalTaskType.SN, ApprovalStatus.APPROVING, 1);
        ApprovalAddSignTask nextSign = sign("next-sign", nextTask.getId(), currentTask.getId(), rootTask.getId(), 200L);
        nextSign.setType(nextSignType.name());
        List<ApprovalAddSignTask> chain = new ArrayList<>();
        if (currentIsSign) {
            ApprovalAddSignTask currentSign = sign("current-sign", currentTask.getId(), rootTask.getId(),
                    rootTask.getId(), 100L);
            when(addSignTaskMapper.selectOne(any())).thenReturn(currentSign);
            chain.add(currentSign);
        }
        chain.add(nextSign);
        when(addSignTaskMapper.selectListByLambda(any())).thenReturn(chain);
        when(taskMapper.selectByPrimaryKey(rootTask.getId())).thenReturn(rootTask);
        mockTaskQuery(List.of(nextTask));

        GenericException exception = assertThrows(GenericException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "revokeProcess", currentTask, instance, "org-1"));

        assertEquals("后续为加签节点，无法撤销", exception.getMessage());
        assertEquals(ApprovalStatus.APPROVED.name(), currentTask.getStatus());
        assertEquals(ApprovalStatus.APPROVING.name(), nextTask.getStatus());
        verify(taskMapper, never()).updateById(any());
        verify(taskMapper, never()).delete(any(ApprovalTask.class));
        verify(taskMapper, never()).insert(any(ApprovalTask.class));
        verify(approvalFlowService, never()).getTaskNextNode(any(), any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "PENDING", "UNAPPROVED", "NONE", "APPROVED", "EXPIRED_ROUND", "OTHER_INSTANCE",
            "OTHER_NODE",
            "CURRENT_NOT_APPROVED", "INSTANCE_NOT_APPROVING", "INSTANCE_OTHER_NODE", "NESTED_BEFORE_ROOT_PROCESSED" })
    void revokeBeforeSignRejectsProcessedOrUnrelatedOriginalTask(String scenario) {
        ApprovalInstance instance = prepareAnyNode(false);
        ApprovalTask rootTask = task("root", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        ApprovalTask currentTask = task("sign-current", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1);
        switch (scenario) {
        case "EXPIRED_ROUND" -> rootTask.setNodeRound(2);
        case "OTHER_INSTANCE" -> rootTask.setInstanceId("instance-2");
        case "OTHER_NODE" -> rootTask.setNodeId("NODE002");
        case "CURRENT_NOT_APPROVED" -> currentTask.setStatus(ApprovalStatus.APPROVING.name());
        case "INSTANCE_NOT_APPROVING" -> instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
        case "INSTANCE_OTHER_NODE" -> instance.setCurrentNodeId("NODE002");
        case "NESTED_BEFORE_ROOT_PROCESSED" -> {
            rootTask.setStatus(ApprovalStatus.APPROVED.name());
            instance.setCurrentNodeId("NODE002");
        }
        default -> rootTask.setStatus(scenario);
        }
        ApprovalAddSignTask sign = sign("current-sign", currentTask.getId(), rootTask.getId(), rootTask.getId(), 100L);
        when(addSignTaskMapper.selectOne(any())).thenReturn(sign);
        if ("NESTED_BEFORE_ROOT_PROCESSED".equals(scenario)) {
            sign.setSignTaskId("sign-parent");
            sign.setType(ApprovalAddSignType.AFTER.name());
            sign.setSort(200L);
            ApprovalAddSignTask parent = sign("parent-sign", "sign-parent", rootTask.getId(), rootTask.getId(), 100L);
            when(addSignTaskMapper.selectListByLambda(any())).thenReturn(List.of(parent, sign));
        } else {
            when(addSignTaskMapper.selectListByLambda(any())).thenReturn(List.of(sign));
        }
        when(taskMapper.selectByPrimaryKey(rootTask.getId())).thenReturn(rootTask);

        GenericException exception = assertThrows(GenericException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "revokeProcess", currentTask, instance, "org-1"));

        assertEquals("无法撤销", exception.getMessage());
        verify(taskMapper, never()).updateById(any());
        verify(taskMapper, never()).delete(any(ApprovalTask.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void revokeOrdinaryOrCompletedAfterSignStillChecksAndClearsNextFlowNode(boolean currentIsSign) {
        ApprovalInstance instance = prepareAnyNode(false);
        instance.setCurrentNodeId("NODE002");
        ApprovalTask rootTask = task("root", ApprovalTaskType.NL, ApprovalStatus.APPROVED, 1);
        ApprovalTask currentTask = currentIsSign
                ? task("sign-current", ApprovalTaskType.SN, ApprovalStatus.APPROVED, 1)
                : rootTask;
        if (currentIsSign) {
            ApprovalAddSignTask sign = sign("current-sign", currentTask.getId(), rootTask.getId(), rootTask.getId(),
                    100L);
            sign.setType(ApprovalAddSignType.AFTER.name());
            when(addSignTaskMapper.selectOne(any())).thenReturn(sign);
            when(addSignTaskMapper.selectListByLambda(any())).thenReturn(List.of(sign));
            when(taskMapper.selectByPrimaryKey(rootTask.getId())).thenReturn(rootTask);
        }
        ApprovalNodeResponse next = new ApprovalNodeResponse();
        next.setId("NODE002");
        next.setNodeType("APPROVER");
        when(approvalFlowService.getTaskNextNode(currentTask, instance, "org-1")).thenReturn(next);
        ApprovalTask nextTask = task("next", ApprovalTaskType.NL, ApprovalStatus.APPROVING, 1);
        nextTask.setNodeId("NODE002");
        mockTaskQuery(List.of(nextTask));
        when(extInstanceMapper.getNodeRound("instance-1", "NODE002")).thenReturn(1);

        ReflectionTestUtils.invokeMethod(service, "revokeProcess", currentTask, instance, "org-1");

        verify(approvalFlowService).getTaskNextNode(currentTask, instance, "org-1");
        verify(extInstanceMapper).batchClearNotApprovingTask("instance-1", "NODE002", 1);
        verify(extInstanceMapper).batchClearRecord("instance-1", "NODE002", 1);
        verify(extInstanceMapper).batchClearApprovingTask("instance-1", "NODE002", 1);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void clearExpiredNodeClearsNonNegativeRounds(int round) {
        when(extInstanceMapper.getNodeRound("instance-1", "NODE001")).thenReturn(round);

        service.clearExpiredNode("instance-1", "NODE001");

        verify(extInstanceMapper).batchClearNotApprovingTask("instance-1", "NODE001", round);
        verify(extInstanceMapper).batchClearRecord("instance-1", "NODE001", round);
        verify(extInstanceMapper).batchClearApprovingTask("instance-1", "NODE001", round);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void loseCurrentNodeClearsNonNegativeRounds(int round) {
        when(extInstanceMapper.getNodeRound("instance-1", "NODE001")).thenReturn(round);

        service.loseCurrentNode("instance-1", "NODE001");

        verify(extInstanceMapper).loseApprovingTask("instance-1", "NODE001", round);
    }

    @Test
    void cleanupSkipsExpiredRound() {
        when(extInstanceMapper.getNodeRound("instance-1", "NODE001")).thenReturn(-1);

        service.clearExpiredNode("instance-1", "NODE001");
        service.loseCurrentNode("instance-1", "NODE001");

        verify(extInstanceMapper, never()).batchClearNotApprovingTask(anyString(), anyString(), anyInt());
        verify(extInstanceMapper, never()).batchClearRecord(anyString(), anyString(), anyInt());
        verify(extInstanceMapper, never()).batchClearApprovingTask(anyString(), anyString(), anyInt());
        verify(extInstanceMapper, never()).loseApprovingTask(anyString(), anyString(), anyInt());
    }

    private void mockTaskQuery(List<ApprovalTask> tasks) {
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            var params = wrapper.getParams();
            return tasks.stream()
                    .filter(task -> !params.containsKey("array_id")
                            || ((List<?>) params.get("array_id")).contains(task.getId()))
                    .filter(task -> !params.containsKey("instance_id_eq")
                            || params.get("instance_id_eq").equals(task.getInstanceId()))
                    .filter(task -> !params.containsKey("node_id_eq")
                            || params.get("node_id_eq").equals(task.getNodeId()))
                    .filter(task -> !params.containsKey("node_round_eq")
                            || params.get("node_round_eq").equals(task.getNodeRound()))
                    .filter(task -> !params.containsKey("status_eq")
                            || params.get("status_eq").toString().equals(task.getStatus()))
                    .filter(task -> !params.containsKey("type_eq")
                            || params.get("type_eq").toString().equals(task.getType()))
                    .toList();
        });
    }

    private ApprovalInstance prepareAnyNode(boolean multiApprover) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-1");
        instance.setSubmitterId("submitter");
        instance.setCurrentNodeId("NODE001");
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        ApprovalNodeApprover node = new ApprovalNodeApprover();
        node.setMultiApproverMode("ANY");
        when(nodeApproverMapper.selectByPrimaryKey("NODE001")).thenReturn(node);
        when(approvalFlowService.isCurrentNodeMultiApprover("NODE001", "submitter", "org-1")).thenReturn(multiApprover);
        ApprovalNodeResponse end = new ApprovalNodeResponse();
        end.setId("END001");
        end.setNodeType("END");
        when(approvalFlowService.getTaskNextNode(any(), any(), anyString())).thenReturn(end);
        when(approvalFlowService.getCurrentNodeCcList("NODE001", "submitter", "org-1")).thenReturn(List.of());
        when(extInstanceMapper.getNodeRound("instance-1", "NODE001")).thenReturn(1);
        return instance;
    }

    private ApprovalInstance prepareApproval(ApprovalTask task, boolean multiApprover) {
        ApprovalInstance instance = prepareAnyNode(multiApprover);
        when(taskMapper.selectByPrimaryKey(task.getId())).thenReturn(task);
        when(extInstanceMapper.selectByTaskIdForUpdate(task.getId())).thenReturn(instance);
        when(extInstanceMapper.getNodeRound(instance.getId(), task.getNodeId())).thenReturn(task.getNodeRound());
        return instance;
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
