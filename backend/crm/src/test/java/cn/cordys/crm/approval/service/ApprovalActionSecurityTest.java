package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalReturnBackRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.approval.dto.ApprovalTaskNode;
import cn.cordys.crm.approval.dto.request.ApprovalActionRequest;
import cn.cordys.crm.approval.dto.request.ApprovalAddSignRequest;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.dto.request.ApprovalRevokeRequest;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.dto.response.ApprovalResubmitNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.EntityTableMapper;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalActionSecurityTest {

    private static ApplicationContext previousContext;
    private static MessageSource previousMessageSource;

    private ApprovalActionService service;
    private BaseMapper<ApprovalInstance> instanceMapper;
    private BaseMapper<ApprovalTask> taskMapper;
    private BaseMapper<ApprovalAddSignTask> signTaskMapper;
    private BaseMapper<ApprovalReturnBackRecord> backRecordMapper;
    private BaseMapper<ApprovalRecord> recordMapper;
    private BaseMapper<ApprovalNode> nodeMapper;
    private BaseMapper<ApprovalNodeApprover> nodeApproverMapper;
    private ExtApprovalInstanceMapper extInstanceMapper;
    private ExtApprovalTaskMapper extTaskMapper;
    private ApprovalFlowService flowService;
    private ApprovalInstanceService instanceService;
    private AttachmentService attachmentService;
    private CommonNoticeSendService noticeService;
    private ApprovalTask task;
    private ApprovalInstance instance;

    @BeforeAll
    static void initContext() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        ReflectionTestUtils.setField(Translator.class, "messageSource", messages);
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
        ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = spy(new ApprovalActionService());
        instanceMapper = mock(BaseMapper.class);
        taskMapper = mock(BaseMapper.class);
        signTaskMapper = mock(BaseMapper.class);
        backRecordMapper = mock(BaseMapper.class);
        recordMapper = mock(BaseMapper.class);
        nodeMapper = mock(BaseMapper.class);
        nodeApproverMapper = mock(BaseMapper.class);
        extInstanceMapper = mock(ExtApprovalInstanceMapper.class);
        extTaskMapper = mock(ExtApprovalTaskMapper.class);
        flowService = mock(ApprovalFlowService.class);
        instanceService = mock(ApprovalInstanceService.class);
        attachmentService = mock(AttachmentService.class);
        noticeService = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "approvalAddSignTaskMapper", signTaskMapper);
        ReflectionTestUtils.setField(service, "approvalAddSignTasMapper", signTaskMapper);
        ReflectionTestUtils.setField(service, "approvalReturnBackRecordMapper", backRecordMapper);
        ReflectionTestUtils.setField(service, "approvalRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "approvalNodeMapper", nodeMapper);
        ReflectionTestUtils.setField(service, "approvalNodeApproverMapper", nodeApproverMapper);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", extInstanceMapper);
        ReflectionTestUtils.setField(service, "extApprovalTaskMapper", extTaskMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(service, "approvalInstanceService", instanceService);
        ReflectionTestUtils.setField(service, "attachmentService", attachmentService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeService);

        task = new ApprovalTask();
        task.setId("task-1");
        task.setInstanceId("instance-1");
        task.setNodeId("node-2");
        task.setApproverId("approver-1");
        task.setType(ApprovalTaskType.NL.name());
        task.setStatus(ApprovalStatus.APPROVING.name());
        task.setNodeRound(2);
        instance = new ApprovalInstance();
        instance.setId(task.getInstanceId());
        instance.setFlowVersionId("version-1");
        instance.setType("contract");
        instance.setSubmitterId("submitter-1");
        instance.setCurrentNodeId(task.getNodeId());
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        when(taskMapper.selectByPrimaryKey(task.getId())).thenReturn(task);
        when(instanceMapper.selectByPrimaryKey(instance.getId())).thenReturn(instance);
        when(extInstanceMapper.selectByTaskIdForUpdate(task.getId(), "org-1")).thenReturn(instance);
        when(extInstanceMapper.getNodeRound(instance.getId(), task.getNodeId())).thenReturn(2);
        ApprovalFlow flow = new ApprovalFlow();
        flow.setAllowAddSign(true);
        flow.setAllowWithdraw(true);
        when(flowService.selectApprovalFlowByFormType(instance.getType(), "org-1")).thenReturn(flow);
    }

    @ParameterizedTest
    @CsvSource({ "SIGN,INSTANCE", "SIGN,NODE", "SIGN,APPROVER", "BACK,INSTANCE", "BACK,NODE", "BACK,APPROVER" })
    void signAndBackRejectMismatchedRequestBeforeWriting(String action, String mismatch) {
        ApprovalActionRequest request = request(action);
        switch (mismatch) {
        case "INSTANCE" -> {
            request.setInstanceId("other-instance");
            when(instanceMapper.selectByPrimaryKey("other-instance")).thenReturn(instance);
        }
        case "NODE" -> request.setNodeId("other-node");
        default -> request.setApproverId("other-approver");
        }

        assertThrows(GenericException.class, () -> execute(action, request));

        assertEquals(ApprovalStatus.APPROVING.name(), task.getStatus());
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = { "SIGN", "BACK", "REVOKE" })
    void mutationsRejectWhenTaskIsOutsideCurrentOrganization(String action) {
        ApprovalActionRequest request = request(action);

        assertThrows(GenericException.class, () -> {
            switch (action) {
            case "SIGN" -> service.sign((ApprovalAddSignRequest) request, task.getApproverId(), "other-org");
            case "BACK" -> service.back((ApprovalReturnBackRequest) request, task.getApproverId(), "other-org");
            default -> {
                ApprovalRevokeRequest revokeRequest = new ApprovalRevokeRequest();
                revokeRequest.setId(task.getId());
                service.revoke(revokeRequest, task.getApproverId(), "other-org");
            }
            }
        });

        verify(extInstanceMapper).selectByTaskIdForUpdate(task.getId(), "other-org");
        assertEquals(ApprovalStatus.APPROVING.name(), task.getStatus());
        assertUnchanged();
        verifyNoInteractions(flowService, instanceService);
    }

    @ParameterizedTest
    @CsvSource({ "SIGN,APPROVED", "SIGN,PENDING", "SIGN,OLD_ROUND", "SIGN,CLEARED_ROUND", "SIGN,OTHER_NODE",
            "SIGN,FINISHED", "BACK,APPROVED", "BACK,PENDING", "BACK,OLD_ROUND", "BACK,CLEARED_ROUND",
            "BACK,OTHER_NODE", "BACK,FINISHED" })
    void signAndBackRejectInactiveTaskBeforeWriting(String action, String scenario) {
        switch (scenario) {
        case "OLD_ROUND" -> task.setNodeRound(1);
        case "CLEARED_ROUND" -> task.setNodeRound(-1);
        case "OTHER_NODE" -> instance.setCurrentNodeId("node-3");
        case "FINISHED" -> instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
        default -> task.setStatus(scenario);
        }
        String originalStatus = task.getStatus();

        assertThrows(GenericException.class, () -> execute(action, request(action)));

        assertEquals(originalStatus, task.getStatus());
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = { "FUTURE", "UNEXECUTED", "OTHER_VERSION", "CURRENT", "INVALID_SIGN", "ORDINARY_TASK_ID" })
    void backRejectsInvalidTargetBeforeWriting(String scenario) {
        ApprovalReturnBackRequest request = (ApprovalReturnBackRequest) request("BACK");
        prepareBackTarget("node-1", List.of(recordNode("node-1", "old-task"),
                recordNode(task.getNodeId(), task.getId())));
        switch (scenario) {
        case "FUTURE" -> prepareBackTarget("node-1",
                List.of(recordNode(task.getNodeId(), task.getId()), recordNode("node-1", "future-task")));
        case "UNEXECUTED" -> prepareBackTarget("node-1", List.of(recordNode(task.getNodeId(), task.getId())));
        case "OTHER_VERSION" -> {
            ApprovalNode node = nodeMapper.selectByPrimaryKey("node-1");
            node.setFlowVersionId("other-version");
        }
        case "CURRENT" -> {
            request.setReturnToNodeId(task.getNodeId());
            prepareBackTarget(task.getNodeId(), List.of(recordNode(task.getNodeId(), task.getId())));
        }
        case "INVALID_SIGN" -> {
            request.setReturnToNodeId("node-1-SN0");
            request.setReturnToTaskId("unrelated-task");
            prepareBackTarget("node-1", List.of(recordNode("node-1-SN0", "old-sign-task"),
                    recordNode(task.getNodeId(), task.getId())));
        }
        default -> request.setReturnToTaskId("unrelated-task");
        }

        assertThrows(GenericException.class, () -> service.back(request, task.getApproverId(), "org-1"));

        assertEquals(ApprovalStatus.APPROVING.name(), task.getStatus());
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = { "ORDINARY", "BEFORE_SIGN", "AFTER_SIGN", "MULTI_SIGN" })
    void resubmitOptionsOnlyIncludeThisRoundsRegularPrefixAndCurrentHost(String scenario) {
        List<ApprovalRecordNode> nodes = new ArrayList<>();
        ApprovalRecordNode prior = recordNode("node-1", "old-task");
        prior.setNodeNumber("P" + "N001");
        prior.setNodeName("主管审核");
        if (!"BEFORE_SIGN".equals(scenario)) {
            nodes.add(prior);
        }
        ApprovalRecordNode historicalSign = recordNode("node-1-SN0", "historical-sign");
        historicalSign.setNodeNumber("P" + "N099");
        nodes.add(historicalSign);
        ApprovalRecordNode current = recordNode(task.getNodeId(), task.getId());
        current.setNodeNumber("P" + "N002");
        current.setNodeName("法务审核");
        if ("BEFORE_SIGN".equals(scenario) || "AFTER_SIGN".equals(scenario)) {
            task.setType(ApprovalTaskType.SN.name());
            current.setNodeId(task.getNodeId() + "-SN1");
        }
        if ("AFTER_SIGN".equals(scenario)) {
            ApprovalRecordNode host = recordNode(task.getNodeId(), "approved-host-task");
            host.setNodeNumber("P" + "N002");
            host.setNodeName("法务审核");
            nodes.add(host);
        }
        if ("MULTI_SIGN".equals(scenario)) {
            task.setType(ApprovalTaskType.SN.name());
            current.setTaskNodes(List.of(ApprovalTaskNode.builder().taskId("host-task").build(),
                    ApprovalTaskNode.builder().taskId(task.getId()).build()));
        }
        nodes.add(current);
        ApprovalRecordNode future = recordNode("node-3", "future-task");
        future.setNodeNumber("P" + "N003");
        nodes.add(future);
        prepareBackTarget("node-1", nodes);

        List<ApprovalResubmitNodeResponse> options = service.getResubmitNodes(task.getId(), task.getApproverId(),
                "org-1");

        assertEquals("BEFORE_SIGN".equals(scenario)
                        ? List.of("", "P" + "N002") : List.of("", "P" + "N001", "P" + "N002"),
                options.stream().map(ApprovalResubmitNodeResponse::getNodeNumber).toList());
        assertEquals("张三", options.getFirst().getNodeName());
        assertEquals("法务审核", options.getLast().getNodeName());
        ArgumentCaptor<ApprovalInstance> copy = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceService).getHistoricalApprovalInstanceDetail(copy.capture(), eq("org-1"));
        assertNotSame(instance, copy.getValue());
        assertEquals(instance.getId(), copy.getValue().getId());
        assertEquals(instance.getSubmitterId(), copy.getValue().getSubmitterId());
        assertEquals("node-2", instance.getCurrentNodeId());
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = { "OTHER_USER", "OTHER_ORG", "NO_USER", "PENDING", "APPROVED", "UNAPPROVED", "CC",
            "OLD_ROUND", "CLEARED_ROUND", "OTHER_NODE", "FINISHED" })
    void resubmitOptionsRejectUnauthorizedAndInactiveTasks(String scenario) {
        switch (scenario) {
        case "NO_USER" -> task.setApproverId(null);
        case "CC" -> task.setType(ApprovalTaskType.CC.name());
        case "OLD_ROUND" -> task.setNodeRound(1);
        case "CLEARED_ROUND" -> task.setNodeRound(-1);
        case "OTHER_NODE" -> instance.setCurrentNodeId("other-node");
        case "FINISHED" -> instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
        case "PENDING", "APPROVED", "UNAPPROVED" -> task.setStatus(scenario);
        default -> {
        }
        }

        assertThrows(GenericException.class, () -> service.getResubmitNodes(task.getId(),
                "OTHER_USER".equals(scenario) ? "other-user" : task.getApproverId(),
                "OTHER_ORG".equals(scenario) ? "other-org" : "org-1"));

        verifyNoInteractions(instanceService);
        assertUnchanged();
    }

    @Test
    void resubmitOptionsDoNotReturnAPrefixWhenCurrentTaskIsMissing() {
        ApprovalRecordNode prior = recordNode("node-1", "old-task");
        prior.setNodeNumber("P" + "N001");
        prepareBackTarget("node-1", List.of(prior));

        assertThrows(GenericException.class,
                () -> service.getResubmitNodes(task.getId(), task.getApproverId(), "org-1"));

        assertUnchanged();
    }

    @ParameterizedTest
    @CsvSource({ "NL,P" + "N001", "NL,P" + "N002", "SN,P" + "N001", "SN,P" + "N002" })
    void rejectSavesRegularStartNodeAndMapsCurrentSignToItsHost(String type, String number) {
        task.setType(type);
        ApprovalRecordNode prior = recordNode("node-1", "old-task");
        prior.setNodeNumber("P" + "N001");
        ApprovalRecordNode current = recordNode(task.getNodeId() + ("SN".equals(type) ? "-SN0" : ""), task.getId());
        current.setNodeNumber("P" + "N002");
        prepareBackTarget("node-1", List.of(prior, current));
        prepareReject();
        ApprovalActionRequest request = rejectRequest(number);

        service.reject(request, task.getApproverId(), "org-1");

        assertEquals(number, instance.getResubmitStartNodeNumber());
        assertEquals("node-2", instance.getCurrentNodeId());
        verify(instanceService).rejectApprovalInstance(instance, task.getApproverId());
        verify(recordMapper).insert(any());
        verify(taskMapper, never()).insert(any());
        verify(taskMapper, never()).batchInsert(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "FUTURE", "SIGN_ONLY", "MISSING" })
    void rejectRefusesFutureAndHistoricalSignTargetsBeforeWriting(String scenario) {
        ApprovalRecordNode current = recordNode(task.getNodeId(), task.getId());
        current.setNodeNumber("P" + "N002");
        ApprovalRecordNode target = recordNode("node-1", "old-task");
        target.setNodeNumber("P" + "N001");
        List<ApprovalRecordNode> nodes;
        if ("FUTURE".equals(scenario)) {
            nodes = List.of(current, target);
        } else if ("SIGN_ONLY".equals(scenario)) {
            target.setNodeId("node-1-SN0");
            nodes = List.of(target, current);
        } else {
            nodes = List.of(current);
        }
        prepareBackTarget("node-1", nodes);

        assertThrows(GenericException.class,
                () -> service.reject(rejectRequest("P" + "N001"), task.getApproverId(), "org-1"));

        assertEquals(ApprovalStatus.APPROVING.name(), task.getStatus());
        assertUnchanged();
        verify(extTaskMapper, never()).rejectPendingTask(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectWithDefaultPathClearsThePreviousResubmitMarker(String number) {
        instance.setResubmitStartNodeNumber("P" + "N001");
        prepareReject();

        service.reject(rejectRequest(number), task.getApproverId(), "org-1");

        assertEquals("", instance.getResubmitStartNodeNumber());
        verify(instanceService, never()).getHistoricalApprovalInstanceDetail(any(ApprovalInstance.class), any());
        BaseMapper.UpdateSqlProvider provider = new BaseMapper.UpdateSqlProvider();
        ReflectionTestUtils.setField(provider, "table", EntityTableMapper.extractTableInfo(ApprovalInstance.class));
        var sql = new XMLLanguageDriver().createSqlSource(new Configuration(), provider.updateSQL(),
                ApprovalInstance.class).getBoundSql(instance);
        assertTrue(sql.getSql().contains("resubmit_start_node_number"));
    }

    private ApprovalActionRequest rejectRequest(String number) {
        return ApprovalActionRequest.builder().id(task.getId()).instanceId(instance.getId())
                .nodeId(task.getNodeId()).approverId(task.getApproverId()).resubmitStartNodeNumber(number).build();
    }

    @Test
    void automaticRejectDoesNotReuseThisRoundsStartAsANewResubmitChoice() {
        instance.setResubmitStartNodeNumber("P" + "N002");
        ApprovalNodeResponse rejected = new ApprovalNodeResponse();
        rejected.setId("auto-reject");
        rejected.setNodeType("EXCEPTION");

        ReflectionTestUtils.invokeMethod(service, "handleNextApprovalNode", rejected, instance,
                task.getApproverId(), "org-1");

        assertEquals(ApprovalStatus.UNAPPROVED.name(), instance.getApprovalStatus());
        assertEquals("", instance.getResubmitStartNodeNumber());
        verify(instanceMapper).updateById(instance);
    }

    private void prepareReject() {
        instance.setResourceId("resource-1");
        instance.setExecuteTime("CREATE");
        when(extTaskMapper.rejectPendingTask(task)).thenReturn(1);
        doNothing().when(service).loseCurrentNode(instance.getId(), task.getNodeId());
        doNothing().when(service).revertFromSnapshot(FormKey.CONTRACT, "CREATE", "resource-1", task.getApproverId(),
                "org-1");
        doNothing().when(service).saveLogAndNotice(instance, task.getApproverId(), "org-1", ApprovalAction.REJECT);
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 1 })
    void revokeRejectsHistoricalRoundBeforeClearingFollowingTasks(int round) {
        task.setStatus(ApprovalStatus.APPROVED.name());
        task.setNodeRound(round);
        instance.setCurrentNodeId("node-3");
        ApprovalRevokeRequest request = new ApprovalRevokeRequest();
        request.setId(task.getId());

        assertThrows(GenericException.class, () -> service.revoke(request, task.getApproverId(), "org-1"));

        assertEquals(ApprovalStatus.APPROVED.name(), task.getStatus());
        assertEquals("node-3", instance.getCurrentNodeId());
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = { "APPROVING", "PENDING", "UNAPPROVED", "NONE", "CC", "FINISHED", "REVOKED" })
    void revokeRejectsUnapprovedTaskOrFinishedInstanceBeforeWriting(String scenario) {
        task.setStatus(ApprovalStatus.APPROVED.name());
        instance.setCurrentNodeId("node-3");
        switch (scenario) {
        case "CC" -> task.setType(ApprovalTaskType.CC.name());
        case "FINISHED" -> instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
        case "REVOKED" -> instance.setApprovalStatus(ApprovalStatus.REVOKED.name());
        default -> task.setStatus(scenario);
        }
        String originalStatus = task.getStatus();
        ApprovalRevokeRequest request = new ApprovalRevokeRequest();
        request.setId(task.getId());

        assertThrows(GenericException.class, () -> service.revoke(request, task.getApproverId(), "org-1"));

        assertEquals(originalStatus, task.getStatus());
        assertEquals("node-3", instance.getCurrentNodeId());
        assertUnchanged();
    }

    @Test
    void revokeCurrentRoundRestoresOrdinaryTaskAndClearsFollowingNode() {
        task.setStatus(ApprovalStatus.APPROVED.name());
        task.setAction(ApprovalAction.APPROVE.name());
        instance.setCurrentNodeId("node-3");
        ApprovalTask nextTask = new ApprovalTask();
        nextTask.setInstanceId(instance.getId());
        nextTask.setNodeId("node-3");
        nextTask.setStatus(ApprovalStatus.APPROVING.name());
        nextTask.setNodeRound(1);
        ApprovalNodeResponse nextNode = new ApprovalNodeResponse();
        nextNode.setId(nextTask.getNodeId());
        nextNode.setNodeType("APPROVER");
        when(flowService.getTaskNextNode(task, instance, "org-1")).thenReturn(nextNode);
        when(taskMapper.selectListByLambda(any())).thenReturn(List.of(nextTask));
        when(extInstanceMapper.getNodeRound(instance.getId(), nextTask.getNodeId())).thenReturn(1);
        doNothing().when(service).saveLogAndNotice(instance, task.getApproverId(), "org-1", ApprovalAction.REVOKE);
        ApprovalRevokeRequest request = new ApprovalRevokeRequest();
        request.setId(task.getId());

        service.revoke(request, task.getApproverId(), "org-1");

        assertEquals(ApprovalStatus.APPROVING.name(), task.getStatus());
        assertEquals(task.getNodeId(), instance.getCurrentNodeId());
        assertEquals(2, task.getNodeRound());
        verify(taskMapper).updateById(task);
        verify(instanceMapper).updateById(instance);
        verify(extInstanceMapper).batchClearNotApprovingTask(instance.getId(), nextTask.getNodeId(), 1);
        verify(extInstanceMapper).batchClearApprovingTask(instance.getId(), nextTask.getNodeId(), 1);
        verify(extInstanceMapper).batchClearRecord(instance.getId(), nextTask.getNodeId(), 1);
        verifyNoInteractions(recordMapper, attachmentService, noticeService);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void backToExecutedOrdinaryNodeCreatesNewTasksBeforeResettingCurrentNode() {
        ApprovalReturnBackRequest request = (ApprovalReturnBackRequest) request("BACK");
        prepareBackTarget("node-1", List.of(recordNode("node-1", "old-task"),
                recordNode(task.getNodeId(), task.getId())));
        ApprovalNodeApprover nodeApprover = new ApprovalNodeApprover();
        nodeApprover.setApprovalType("MANUAL");
        nodeApprover.setSameSubmitterAction("ALLOW");
        nodeApprover.setMultiApproverMode("ALL");
        when(nodeApproverMapper.selectByPrimaryKey("node-1")).thenReturn(nodeApprover);
        when(flowService.getCurrentNodeApproverList(instance, "node-1", "org-1")).thenReturn(List.of("prior-approver"));
        when(extInstanceMapper.getNextNodeRound(instance.getId(), "node-1")).thenReturn(3);
        ApprovalNodeResponse next = new ApprovalNodeResponse();
        next.setId(task.getNodeId());
        when(flowService.getCurrentNextNode("node-1", instance, "org-1")).thenReturn(next);
        doNothing().when(service).saveLogAndNotice(instance, task.getApproverId(), "org-1", ApprovalAction.BACK);

        service.back(request, task.getApproverId(), "org-1");

        assertEquals("node-1", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.PENDING.name(), task.getStatus());
        ArgumentCaptor<List<ApprovalTask>> tasks = ArgumentCaptor.forClass((Class) List.class);
        verify(taskMapper).batchInsert(tasks.capture());
        assertEquals(1, tasks.getValue().size());
        ApprovalTask priorTask = tasks.getValue().getFirst();
        assertEquals("node-1", priorTask.getNodeId());
        assertEquals("prior-approver", priorTask.getApproverId());
        assertEquals(3, priorTask.getNodeRound());
        assertEquals(ApprovalStatus.APPROVING.name(), priorTask.getStatus());
        ArgumentCaptor<ApprovalInstance> copy = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceService).getApprovalInstanceDetail(copy.capture(), eq("org-1"));
        assertNotSame(instance, copy.getValue());
        assertEquals("node-2", copy.getValue().getCurrentNodeId());
        verify(instanceMapper).updateById(instance);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SKIP", "ASSIGN_SUPERIOR" })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void backToAutomaticallyApprovedNodeKeepsNextTaskOperable(String sameSubmitterAction) {
        ApprovalReturnBackRequest request = (ApprovalReturnBackRequest) request("BACK");
        prepareBackTarget("node-1", List.of(recordNode("node-1", "old-task"),
                recordNode(task.getNodeId(), task.getId())));
        ApprovalNodeApprover config = new ApprovalNodeApprover();
        config.setApprovalType("MANUAL");
        config.setSameSubmitterAction(sameSubmitterAction);
        config.setMultiApproverMode("ALL");
        when(nodeApproverMapper.selectByPrimaryKey("node-1")).thenReturn(config);
        when(nodeApproverMapper.selectByPrimaryKey(task.getNodeId())).thenReturn(config);
        when(flowService.getCurrentNodeApproverList(instance, "node-1", "org-1"))
                .thenReturn(List.of(instance.getSubmitterId()));
        when(extInstanceMapper.getNextNodeRound(instance.getId(), "node-1")).thenReturn(3);
        when(extInstanceMapper.getNextNodeRound(instance.getId(), task.getNodeId())).thenReturn(3);
        ApprovalNodeApproverResponse next = new ApprovalNodeApproverResponse();
        next.setId(task.getNodeId());
        next.setNodeType("APPROVER");
        next.setSameSubmitterAction("ALLOW");
        next.setMultiApproverMode("ALL");
        next.setApproverList(List.of(task.getApproverId()));
        next.setAutoSkipUserIds(List.of());
        when(flowService.getCurrentNextNode("node-1", instance, "org-1")).thenReturn(next);
        when(flowService.getTaskNextNode(any(), eq(instance), eq("org-1"))).thenReturn(next);
        doNothing().when(service).saveLogAndNotice(instance, task.getApproverId(), "org-1", ApprovalAction.BACK);

        service.back(request, task.getApproverId(), "org-1");

        ArgumentCaptor<List<ApprovalTask>> batches = ArgumentCaptor.forClass((Class) List.class);
        verify(taskMapper, times(2)).batchInsert(batches.capture());
        ApprovalTask returned = batches.getAllValues().getFirst().getFirst();
        ApprovalTask pending = batches.getAllValues().getLast().getFirst();
        assertEquals(ApprovalStatus.AUTO_APPROVED.name(), returned.getStatus());
        assertEquals(task.getNodeId(), instance.getCurrentNodeId());
        assertEquals(task.getNodeId(), pending.getNodeId());
        assertEquals(ApprovalStatus.APPROVING.name(), pending.getStatus());
        assertEquals(3, pending.getNodeRound());
        InOrder order = inOrder(service, taskMapper);
        order.verify(service).clearExpiredNode(instance.getId(), task.getNodeId());
        order.verify(taskMapper).batchInsert(batches.getAllValues().getFirst());
        verify(service).clearExpiredNode(instance.getId(), task.getNodeId());

        when(taskMapper.selectByPrimaryKey(pending.getId())).thenReturn(pending);
        when(extInstanceMapper.selectByTaskIdForUpdate(pending.getId(), "org-1")).thenReturn(instance);
        when(extInstanceMapper.getNodeRound(instance.getId(), pending.getNodeId())).thenReturn(3);
        when(extTaskMapper.approvePendingTask(pending)).thenReturn(1);
        ApprovalNodeResponse end = new ApprovalNodeResponse();
        end.setId("end");
        end.setNodeType("END");
        when(flowService.getTaskNextNode(pending, instance, "org-1")).thenReturn(end);
        ApprovalActionRequest approve = ApprovalActionRequest.builder().id(pending.getId())
                .instanceId(instance.getId()).nodeId(pending.getNodeId()).approverId(pending.getApproverId()).build();

        service.approve(approve, pending.getApproverId(), "org-1");

        assertEquals(ApprovalStatus.APPROVED.name(), pending.getStatus());
        assertEquals(ApprovalStatus.APPROVED.name(), instance.getApprovalStatus());
        assertEquals("end", instance.getCurrentNodeId());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void backToPreviousSignInSameFlowNodeKeepsTheSignChain() {
        ApprovalReturnBackRequest request = (ApprovalReturnBackRequest) request("BACK");
        request.setReturnToNodeId(task.getNodeId() + "-SN0");
        request.setReturnToTaskId("old-sign");
        prepareBackTarget(task.getNodeId(), List.of(recordNode(request.getReturnToNodeId(), "old-sign"),
                recordNode(task.getNodeId(), task.getId())));
        ApprovalNodeApprover nodeApprover = new ApprovalNodeApprover();
        nodeApprover.setApprovalType("MANUAL");
        when(nodeApproverMapper.selectByPrimaryKey(task.getNodeId())).thenReturn(nodeApprover);
        ApprovalTask oldSign = new ApprovalTask();
        oldSign.setId("old-sign");
        oldSign.setInstanceId(instance.getId());
        oldSign.setNodeId(task.getNodeId());
        oldSign.setNodeRound(2);
        oldSign.setApproverId("sign-approver");
        oldSign.setType(ApprovalTaskType.SN.name());
        oldSign.setStatus(ApprovalStatus.APPROVED.name());
        when(taskMapper.selectByPrimaryKey(oldSign.getId())).thenReturn(oldSign);
        ApprovalAddSignTask sign = new ApprovalAddSignTask();
        sign.setId("sign-relation");
        sign.setTaskId(oldSign.getId());
        sign.setRootTaskId(task.getId());
        sign.setSignTaskId(task.getId());
        sign.setType("BEFORE");
        sign.setSort(100L);
        when(signTaskMapper.selectOne(any())).thenReturn(sign);
        when(signTaskMapper.selectListByLambda(any())).thenReturn(List.of(sign));
        when(extInstanceMapper.getNextNodeRound(instance.getId(), task.getNodeId())).thenReturn(3);
        doNothing().when(service).saveLogAndNotice(instance, task.getApproverId(), "org-1", ApprovalAction.BACK);

        service.back(request, task.getApproverId(), "org-1");

        ArgumentCaptor<List<ApprovalTask>> tasks = ArgumentCaptor.forClass((Class) List.class);
        verify(taskMapper).batchInsert(tasks.capture());
        assertEquals(2, tasks.getValue().size());
        ApprovalTask newRoot = tasks.getValue().getFirst();
        ApprovalTask newSign = tasks.getValue().getLast();
        assertEquals(ApprovalStatus.PENDING.name(), newRoot.getStatus());
        assertEquals(ApprovalStatus.APPROVING.name(), newSign.getStatus());
        assertEquals(3, newRoot.getNodeRound());
        assertEquals(3, newSign.getNodeRound());
        assertNotEquals(task.getId(), newRoot.getId());
        assertNotEquals(oldSign.getId(), newSign.getId());
        assertEquals(newSign.getId(), sign.getTaskId());
        verify(extTaskMapper).moveAddSignRoot(task.getId(), newRoot.getId());
        verify(instanceMapper).updateById(instance);
    }

    @Test
    void afterSignOnMultiAnyNodeAdvancesWhileNewSignTaskRemainsPending() {
        ApprovalAddSignRequest request = (ApprovalAddSignRequest) request("SIGN");
        request.setType("AFTER");
        ApprovalNodeApprover nodeApprover = new ApprovalNodeApprover();
        nodeApprover.setMultiApproverMode("ANY");
        when(nodeApproverMapper.selectByPrimaryKey(task.getNodeId())).thenReturn(nodeApprover);
        when(flowService.isCurrentNodeMultiApprover(task.getNodeId(), instance.getSubmitterId(), "org-1"))
                .thenReturn(true);
        ApprovalNodeApproverResponse nextNode = new ApprovalNodeApproverResponse();
        nextNode.setId("node-3");
        nextNode.setNodeType("APPROVER");
        when(flowService.getTaskNextNode(any(ApprovalTask.class), eq(instance), eq("org-1"))).thenReturn(nextNode);
        doNothing().when(service).handlerNextNodeApproverTasks(nextNode, instance, task.getApproverId(),
                ApprovalTaskType.NL.name(), "org-1");
        doNothing().when(service).saveLogAndNotice(instance, task.getApproverId(), "org-1", ApprovalAction.SIGN);

        service.sign(request, task.getApproverId(), "org-1");

        verify(flowService).isCurrentNodeMultiApprover(task.getNodeId(), instance.getSubmitterId(), "org-1");
        assertEquals(ApprovalStatus.APPROVED.name(), task.getStatus());
        assertEquals("node-3", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
        ArgumentCaptor<ApprovalTask> appended = ArgumentCaptor.forClass(ApprovalTask.class);
        verify(taskMapper).insert(appended.capture());
        assertEquals(ApprovalTaskType.SN.name(), appended.getValue().getType());
        assertEquals(ApprovalStatus.APPROVING.name(), appended.getValue().getStatus());
        assertEquals(task.getNodeRound(), appended.getValue().getNodeRound());
        verify(instanceMapper).updateById(instance);
        verify(flowService).updateApprovalPostField(instance, task.getNodeId(), ApprovalAction.APPROVE,
                task.getApproverId());
        verify(service).handlerNextNodeApproverTasks(nextNode, instance, task.getApproverId(),
                ApprovalTaskType.NL.name(), "org-1");
    }

    private void prepareBackTarget(String nodeId, List<ApprovalRecordNode> records) {
        ApprovalNode node = new ApprovalNode();
        node.setId(nodeId);
        node.setFlowVersionId(instance.getFlowVersionId());
        node.setNodeType("APPROVER");
        when(nodeMapper.selectByPrimaryKey(nodeId)).thenReturn(node);
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setSubmitter("张三");
        detail.setNodes(records);
        when(instanceService.getApprovalInstanceDetail(any(ApprovalInstance.class), eq("org-1"))).thenReturn(detail);
        when(instanceService.getHistoricalApprovalInstanceDetail(any(ApprovalInstance.class), eq("org-1")))
                .thenReturn(detail);
    }

    private ApprovalRecordNode recordNode(String nodeId, String taskId) {
        return ApprovalRecordNode.builder().nodeId(nodeId).taskNodes(List.of(
                ApprovalTaskNode.builder().taskId(taskId).build())).build();
    }

    private ApprovalActionRequest request(String action) {
        ApprovalActionRequest request;
        if ("SIGN".equals(action)) {
            ApprovalAddSignRequest signRequest = new ApprovalAddSignRequest();
            signRequest.setType("BEFORE");
            signRequest.setSignApprover("sign-approver");
            request = signRequest;
        } else {
            ApprovalReturnBackRequest backRequest = new ApprovalReturnBackRequest();
            backRequest.setReturnToNodeId("node-1");
            request = backRequest;
        }
        request.setId(task.getId());
        request.setInstanceId(instance.getId());
        request.setNodeId(task.getNodeId());
        request.setApproverId(task.getApproverId());
        return request;
    }

    private void execute(String action, ApprovalActionRequest request) {
        if ("SIGN".equals(action)) {
            service.sign((ApprovalAddSignRequest) request, task.getApproverId(), "org-1");
        } else {
            service.back((ApprovalReturnBackRequest) request, task.getApproverId(), "org-1");
        }
    }

    private void assertUnchanged() {
        verify(taskMapper, never()).updateById(any());
        verify(taskMapper, never()).insert(any());
        verify(taskMapper, never()).batchInsert(any());
        verify(instanceMapper, never()).updateById(any());
        verify(signTaskMapper, never()).insert(any());
        verify(signTaskMapper, never()).updateById(any());
        verify(extInstanceMapper, never()).batchClearNotApprovingTask(any(), any(), any());
        verify(extInstanceMapper, never()).batchClearApprovingTask(any(), any(), any());
        verify(extInstanceMapper, never()).batchClearRecord(any(), any(), any());
        verifyNoInteractions(backRecordMapper, recordMapper, extTaskMapper, attachmentService, noticeService);
        verify(service, never()).saveLogAndNotice(any(), any(), any(), any(ApprovalAction.class));
    }
}
