package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.constants.ApprovalTypeEnum;
import cn.cordys.crm.approval.constants.ApproverTypeEnum;
import cn.cordys.crm.approval.constants.DuplicateApproverRuleEnum;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
import cn.cordys.crm.approval.constants.SameSubmitterActionEnum;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ApprovalAutoPassFlowTest {

    private static ApplicationContext previousContext;
    private static MessageSource previousMessageSource;
    private static DefaultUidGenerator generator;

    private final ApprovalFlowService flowService = spy(new ApprovalFlowService());
    private final ApprovalActionService actionService = new ApprovalActionService();
    private final ApprovalResourceService resourceService = spy(new ApprovalResourceService());
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalTask> taskMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalInstance> instanceMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalNodeApprover> nodeMapper = mock(BaseMapper.class);
    private final ExtApprovalInstanceMapper extInstanceMapper = mock(ExtApprovalInstanceMapper.class);
    private final CommonNoticeSendService notices = mock(CommonNoticeSendService.class);
    private final List<ApprovalTask> tasks = new ArrayList<>();
    private final Map<String, ApprovalNodeResponse> nextNodes = new HashMap<>();
    private final Map<String, ApprovalNodeApproverResponse> nodes = new HashMap<>();
    private final ApprovalInstance instance = new ApprovalInstance();
    private final AtomicReference<String> resourceStatus = new AtomicReference<>(ApprovalStatus.APPROVING.name());

    @BeforeAll
    static void initContext() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("opportunity", Locale.SIMPLIFIED_CHINESE, "商机");
        ReflectionTestUtils.setField(Translator.class, "messageSource", messages);
        generator = new DefaultUidGenerator();
        ReflectionTestUtils.setField(generator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
        generator.init();
    }

    @AfterAll
    static void restoreContext() {
        new CommonBeanFactory().setApplicationContext(previousContext);
        ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(DefaultUidGenerator.class)).thenReturn(generator);
        when(context.getBean(ApprovalResourceService.class)).thenReturn(resourceService);
        new CommonBeanFactory().setApplicationContext(context);
        instance.setId("instance-1");
        instance.setType(FormKey.OPPORTUNITY.getKey());
        instance.setResourceId("opportunity-1");
        instance.setFlowVersionId("version-1");
        instance.setSubmitterId("submitter");
        instance.setCurrentNodeId("NODE001");
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        instance.setExecuteTime(ExecuteTimingEnum.CREATE.name());
        ReflectionTestUtils.setField(actionService, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(actionService, "approvalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(actionService, "approvalNodeApproverMapper", nodeMapper);
        ReflectionTestUtils.setField(actionService, "extApprovalInstanceMapper", extInstanceMapper);
        ReflectionTestUtils.setField(actionService, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(actionService, "commonNoticeSendService", notices);
        ReflectionTestUtils.setField(flowService, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(flowService, "approvalRecordMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(flowService, "extApprovalInstanceMapper", extInstanceMapper);
        ModuleFormService formService = mock(ModuleFormService.class);
        ReflectionTestUtils.setField(flowService, "formService", formService);
        when(formService.compressResourceDetail(anyString(), anyString())).thenReturn(List.of());
        ReflectionTestUtils.setField(resourceService, "extApprovalInstanceMapper", extInstanceMapper);
        ReflectionTestUtils.setField(resourceService, "logService", mock(LogService.class));
        doReturn("示例商机").when(resourceService).getInstanceResourceName(FormKey.OPPORTUNITY, "opportunity-1");
        doNothing().when(flowService).updateApprovalPostField(any(), anyString(), any(), anyString());
        doReturn(DuplicateApproverRuleEnum.SEQUENTIAL_ALL).when(flowService).getFlowDuplicateApproverRule("version-1");
        doAnswer(invocation -> nextNodes.get(invocation.getArgument(0)))
                .when(flowService).getNextNode(anyString(), anyList(), anySet());
        doAnswer(invocation -> ((List<String>) invocation.getArgument(1)).stream().map(id -> {
            User user = new User();
            user.setId(id);
            return user;
        }).toList()).when(flowService).getCurrentNodeApproverList(anyString(), anyList(), anyString(), anyString(),
                any());
        doAnswer(invocation -> nodes.get(invocation.getArgument(1)).getApproverList())
                .when(flowService).getCurrentNodeApproverList(any(ApprovalInstance.class), anyString(), anyString());
        doAnswer(invocation -> nodes.get(invocation.getArgument(0)).getApproverList().size() > 1)
                .when(flowService).isCurrentNodeMultiApprover(anyString(), anyString(), anyString());
        doReturn(List.of()).when(flowService).getCurrentNodeCcList(anyString(), anyString(), anyString());
        doReturn(List.of()).when(flowService).getCurrentNodeCcList(any(), anyList(), anyString(), anyString(), any());
        when(extInstanceMapper.getNodeRound(anyString(), anyString())).thenReturn(1);
        when(extInstanceMapper.getNextNodeRound(anyString(), anyString())).thenReturn(1);
        when(instanceMapper.selectByPrimaryKey("instance-1")).thenReturn(instance);
        when(taskMapper.insert(any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return 1;
        });
        when(taskMapper.batchInsert(anyList())).thenAnswer(invocation -> {
            tasks.addAll(invocation.getArgument(0));
            return 1;
        });
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            Map<String, Object> params = wrapper.getParams();
            return tasks.stream()
                    .filter(task -> !params.containsKey("node_id_eq")
                            || params.get("node_id_eq").equals(task.getNodeId()))
                    .filter(task -> !params.containsKey("node_round_eq")
                            || params.get("node_round_eq").equals(task.getNodeRound()))
                    .filter(task -> !params.containsKey("type_eq")
                            || params.get("type_eq").toString().equals(task.getType()))
                    .filter(task -> !params.containsKey("status_eq")
                            || params.get("status_eq").toString().equals(task.getStatus()))
                    .filter(task -> !params.containsKey("array_status")
                            || ((List<?>) params.get("array_status")).contains(task.getStatus()))
                    .toList();
        });
        when(extInstanceMapper.selectApprovalStatusForUpdate("opportunity", "opportunity-1"))
                .thenAnswer(invocation -> resourceStatus.get());
        doAnswer(invocation -> {
            resourceStatus.set(invocation.getArgument(2));
            return null;
        }).when(extInstanceMapper).updateApprovalStatus(eq("opportunity"), eq("opportunity-1"), anyString());
        ApprovalNodeResponse end = new ApprovalNodeResponse();
        end.setId("END001");
        end.setNodeType(ApprovalNodeTypeEnum.END.name());
        nextNodes.put("NODE003", end);
    }

    @Test
    void automaticNodeBreaksConsecutiveApprovalAndLastManualApprovalCompletesOpportunity() {
        ApprovalTask first = firstApprovedTask(List.of("approver-a"));
        ApprovalNodeApproverResponse automatic = node("AUTO001", MultiApproverModeEnum.ANY, List.of());
        automatic.setApprovalType(ApprovalTypeEnum.AUTO_PASS.name());
        ApprovalNodeApproverResponse last = node("NODE003", MultiApproverModeEnum.ANY, List.of("approver-a"));
        nextNodes.put("NODE001", automatic);
        nextNodes.put("AUTO001", last);

        process(first);

        ApprovalTask pending = tasks.getLast();
        assertEquals("NODE003", pending.getNodeId());
        assertEquals(ApprovalStatus.APPROVING.name(), pending.getStatus());
        assertEquals(List.of(), last.getAutoSkipUserIds());
        assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
        assertEquals(ApprovalStatus.APPROVING.name(), resourceStatus.get());
        pending.setStatus(ApprovalStatus.APPROVED.name());
        process(pending);
        assertCompleted();
    }

    @Test
    void resubmitStartsAtMatchedNodeItselfAndScopesNumberByVersionAndTiming() {
        BaseMapper<ApprovalNode> baseNodeMapper = prepareResubmitNode(ApprovalTypeEnum.MANUAL);

        ApprovalNodeApproverResponse result = (ApprovalNodeApproverResponse) flowService
                .getResourceApprovalInstanceResubmitNode(instance, "P" + "N052", "org-1");

        assertEquals("NODE003", result.getId());
        assertEquals(List.of("approver-b"), result.getApproverList());
        assertEquals(List.of(), result.getAutoSkipUserIds());
        ArgumentCaptor<ApprovalNode> criteria = ArgumentCaptor.forClass(ApprovalNode.class);
        verify(baseNodeMapper).select(criteria.capture());
        assertEquals("version-1", criteria.getValue().getFlowVersionId());
        assertEquals("CREATE", criteria.getValue().getExecuteTime());
        assertEquals("P" + "N052", criteria.getValue().getNumber());
        assertEquals("APPROVER", criteria.getValue().getNodeType());
        verifyNoInteractions(taskMapper);
        verify(flowService, never()).getNextNode(anyString(), anyList(), anySet());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 2 })
    void resubmitDoesNotChooseMissingOrAmbiguousNode(int matches) {
        BaseMapper<ApprovalNode> baseNodeMapper = prepareResubmitNode(ApprovalTypeEnum.MANUAL);
        when(baseNodeMapper.select(any(ApprovalNode.class))).thenReturn(matches == 0 ? List.of()
                : List.of(new ApprovalNode(), new ApprovalNode()));

        assertNull(flowService.getResourceApprovalInstanceResubmitNode(instance, "P" + "N052", "org-1"));

        verifyNoInteractions(taskMapper);
        verify(flowService, never()).getNextNode(anyString(), anyList(), anySet());
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalTypeEnum.class, names = { "AUTO_PASS", "AUTO_REJECT" })
    @SuppressWarnings("unchecked")
    void resubmitRunsAutomaticStartNodeAndRecordsOnlyThisRound(ApprovalTypeEnum type) {
        prepareResubmitNode(type);
        ApplicationContext context = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class,
                "context");
        when(context.getBean(ApprovalActionService.class)).thenReturn(actionService);

        ApprovalNodeResponse result = flowService.getResourceApprovalInstanceResubmitNode(instance, "P" + "N052", "org-1");

        boolean passed = type == ApprovalTypeEnum.AUTO_PASS;
        assertEquals(passed ? "END" : "EXCEPTION", result.getNodeType());
        BaseMapper<ApprovalRecord> records = (BaseMapper<ApprovalRecord>) ReflectionTestUtils.getField(flowService,
                "approvalRecordMapper");
        ArgumentCaptor<ApprovalRecord> record = ArgumentCaptor.forClass(ApprovalRecord.class);
        verify(records).insert(record.capture());
        assertEquals("instance-1", record.getValue().getInstanceId());
        assertEquals("NODE003", record.getValue().getNodeId());
        assertEquals(passed ? "AUTO_APPROVED" : "AUTO_UNAPPROVED", record.getValue().getResult());
        verify(flowService).updateApprovalPostField(eq(instance), eq("NODE003"),
                eq(passed ? ApprovalAction.APPROVE : ApprovalAction.REJECT), anyString());
        verify(flowService, never()).getNextNode(eq("NODE001"), anyList(), anySet());
        verifyNoInteractions(taskMapper);
    }

    @SuppressWarnings("unchecked")
    private BaseMapper<ApprovalNode> prepareResubmitNode(ApprovalTypeEnum type) {
        node("NODE003", MultiApproverModeEnum.ANY, List.of("approver-b"));
        ApprovalNodeApprover config = nodeMapper.selectByPrimaryKey("NODE003");
        config.setId("NODE003");
        config.setFlowVersionId("version-1");
        config.setApprovalType(type.name());
        config.setApproverType(ApproverTypeEnum.MEMBER.name());
        config.setApproverList(JSON.toJSONString(List.of("approver-b")));
        config.setCcList("[]");
        ReflectionTestUtils.setField(flowService, "approvalNodeApproverMapper", nodeMapper);
        ReflectionTestUtils.setField(flowService, "extUserMapper", mock(ExtUserMapper.class));
        ApprovalNode node = new ApprovalNode();
        node.setId("NODE003");
        node.setFlowVersionId("version-1");
        node.setNumber("P" + "N052");
        node.setNodeType("APPROVER");
        node.setExecuteTime("CREATE");
        BaseMapper<ApprovalNode> baseNodeMapper = mock(BaseMapper.class);
        when(baseNodeMapper.select(any(ApprovalNode.class))).thenReturn(List.of(node));
        ReflectionTestUtils.setField(flowService, "approvalNodeMapper", baseNodeMapper);
        return baseNodeMapper;
    }

    @Test
    void firstOnlyStillSkipsRepeatedApproverAcrossAutomaticNode() {
        doReturn(DuplicateApproverRuleEnum.FIRST_ONLY).when(flowService).getFlowDuplicateApproverRule("version-1");
        ApprovalTask first = firstApprovedTask(List.of("approver-a"));
        ApprovalNodeApproverResponse automatic = node("AUTO001", MultiApproverModeEnum.ANY, List.of());
        automatic.setApprovalType(ApprovalTypeEnum.AUTO_PASS.name());
        nextNodes.put("NODE001", automatic);
        nextNodes.put("AUTO001", node("NODE003", MultiApproverModeEnum.ANY, List.of("approver-a")));

        process(first);

        assertEquals(ApprovalStatus.AUTO_APPROVED.name(), tasks.getLast().getStatus());
        assertCompleted();
    }

    @ParameterizedTest
    @ValueSource(strings = { "DIRECT", "CONDITION", "DEFAULT" })
    void consecutiveManualApproversContinueThroughAutomaticAgreementAndConditionBranches(String branch) {
        ApprovalTask first = firstApprovedTask(List.of("approver-a"));
        ApprovalNodeApproverResponse second = node("NODE002", MultiApproverModeEnum.ANY, List.of("approver-a"));
        nextNodes.put("NODE001", second);
        if (!"DIRECT".equals(branch)) {
            ApprovalNodeResponse condition = new ApprovalNodeResponse();
            condition.setId("CONDITION001");
            condition.setNodeType(branch);
            nextNodes.put("NODE001", condition);
            nextNodes.put(condition.getId(), second);
        }
        nextNodes.put("NODE002", node("NODE003", MultiApproverModeEnum.ANY, List.of("approver-a")));

        process(first);

        assertEquals(2,
                tasks.stream().filter(task -> ApprovalStatus.AUTO_APPROVED.name().equals(task.getStatus())).count());
        assertCompleted();
    }

    @ParameterizedTest
    @EnumSource(value = MultiApproverModeEnum.class, names = { "ALL", "SEQUENTIAL" })
    void fullyAutomaticMultiApproverTasksContinueAndUpdateInstanceAndOpportunity(MultiApproverModeEnum mode) {
        ApprovalTask first = firstApprovedTask(List.of("approver-a", "approver-b"));
        nextNodes.put("NODE001", node("NODE003", mode, List.of("approver-a", "approver-b")));

        process(first);

        assertEquals(4, tasks.size());
        assertEquals(2,
                tasks.stream().filter(task -> ApprovalStatus.AUTO_APPROVED.name().equals(task.getStatus())).count());
        assertCompleted();
        verifyNoInteractions(notices);
    }

    @Test
    void partiallySkippedMultiApproverNodeOnlyNotifiesPendingApprovers() {
        ApprovalTask first = firstApprovedTask(List.of("approver-a"));
        nextNodes.put("NODE001", node("NODE003", MultiApproverModeEnum.ALL, List.of("approver-a", "approver-b")));

        process(first);

        assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
        verify(notices).sendNotice(eq(NotificationConstants.Module.APPROVAL),
                eq(NotificationConstants.Event.APPROVAL_TODO),
                anyMap(), eq("submitter"), eq("org-1"), eq(List.of("approver-b")), eq(true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "FIRST", "MIDDLE", "LAST" })
    void missingSupervisorOnlySkipsSubmitterAndPreservesSequentialApprovers(String position) {
        List<String> approvers = switch (position) {
        case "FIRST" -> List.of("submitter", "approver-b");
        case "MIDDLE" -> List.of("approver-a", "submitter", "approver-b");
        default -> List.of("approver-a", "approver-b", "submitter");
        };
        enterNodeWithSubmitter(MultiApproverModeEnum.SEQUENTIAL, approvers, null);

        for (String approver : approvers.stream().filter(id -> !id.equals("submitter")).toList()) {
            assertEquals("NODE003", instance.getCurrentNodeId());
            assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
            assertEquals(ApprovalStatus.APPROVING.name(), resourceStatus.get());
            List<ApprovalTask> pending = tasks.stream()
                    .filter(task -> ApprovalStatus.APPROVING.name().equals(task.getStatus())).toList();
            assertEquals(List.of(approver), pending.stream().map(ApprovalTask::getApproverId).toList());
            verify(notices).sendNotice(eq(NotificationConstants.Module.APPROVAL),
                    eq(NotificationConstants.Event.APPROVAL_TODO),
                    anyMap(), eq("submitter"), eq("org-1"), eq(List.of(approver)), eq(true));
            verifyNoMoreInteractions(notices);
            clearInvocations(notices);
            pending.getFirst().setStatus(ApprovalStatus.APPROVED.name());
            process(pending.getFirst());
        }

        List<ApprovalTask> submitterTasks = tasks.stream().filter(task -> "submitter".equals(task.getApproverId()))
                .toList();
        assertEquals(1, submitterTasks.size());
        assertEquals(ApprovalStatus.AUTO_APPROVED.name(), submitterTasks.getFirst().getStatus());
        assertCompleted();
        verifyNoInteractions(notices);
    }

    @Test
    void missingSupervisorKeepsOtherCountersignApproversPending() {
        enterNodeWithSubmitter(MultiApproverModeEnum.ALL, List.of("submitter", "approver-b"), null);

        assertEquals("NODE003", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
        assertEquals(ApprovalStatus.APPROVING.name(), resourceStatus.get());
        ApprovalTask submitter = tasks.stream().filter(task -> "submitter".equals(task.getApproverId())).findFirst()
                .orElseThrow();
        assertEquals(ApprovalStatus.AUTO_APPROVED.name(), submitter.getStatus());
        ApprovalTask pending = tasks.getLast();
        assertEquals("approver-b", pending.getApproverId());
        assertEquals(ApprovalStatus.APPROVING.name(), pending.getStatus());
        verify(notices).sendNotice(eq(NotificationConstants.Module.APPROVAL),
                eq(NotificationConstants.Event.APPROVAL_TODO),
                anyMap(), eq("submitter"), eq("org-1"), eq(List.of("approver-b")), eq(true));
        verifyNoMoreInteractions(notices);

        pending.setStatus(ApprovalStatus.APPROVED.name());
        process(pending);
        assertCompleted();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void missingSupervisorStillCompletesSingleApproverAndAnyApproval(boolean singleApprover) {
        enterNodeWithSubmitter(MultiApproverModeEnum.ANY,
                singleApprover ? List.of("submitter") : List.of("submitter", "approver-b"), null);

        assertEquals("submitter", tasks.getLast().getApproverId());
        assertEquals(ApprovalStatus.AUTO_APPROVED.name(), tasks.getLast().getStatus());
        assertCompleted();
        verifyNoInteractions(notices);
    }

    @Test
    void existingSupervisorReceivesSequentialTaskBeforeOtherApprovers() {
        enterNodeWithSubmitter(MultiApproverModeEnum.SEQUENTIAL, List.of("submitter", "approver-b"), "supervisor");

        for (String approver : List.of("supervisor", "approver-b")) {
            assertEquals(ApprovalStatus.APPROVING.name(), instance.getApprovalStatus());
            assertEquals(ApprovalStatus.APPROVING.name(), resourceStatus.get());
            ApprovalTask pending = tasks.getLast();
            assertEquals(approver, pending.getApproverId());
            assertEquals(ApprovalStatus.APPROVING.name(), pending.getStatus());
            verify(notices).sendNotice(eq(NotificationConstants.Module.APPROVAL),
                    eq(NotificationConstants.Event.APPROVAL_TODO),
                    anyMap(), eq("submitter"), eq("org-1"), eq(List.of(approver)), eq(true));
            clearInvocations(notices);
            pending.setStatus(ApprovalStatus.APPROVED.name());
            process(pending);
        }

        assertTrue(tasks.stream().noneMatch(task -> "submitter".equals(task.getApproverId())));
        assertCompleted();
    }

    @ParameterizedTest
    @EnumSource(MultiApproverModeEnum.class)
    void supervisorReplacementKeepsUniqueApproversInOriginalOrder(MultiApproverModeEnum mode) {
        List<String> original = List.of("supervisor", "submitter", "approver-b");
        enterNodeWithSubmitter(mode, original, "supervisor");

        List<String> expected = List.of("supervisor", "approver-b");
        assertEquals(expected, nodes.get("NODE003").getApproverList());
        assertEquals(expected, flowService.getCurrentNodeApproverList(instance, "NODE003", "org-1"));
        ApprovalNodeApproverResponse preview = new ApprovalNodeApproverResponse();
        preview.setApprovalType(ApprovalTypeEnum.MANUAL.name());
        preview.setApproverType(ApproverTypeEnum.MEMBER.name());
        preview.setApproverList(original);
        preview.setCcList(List.of());
        preview.setSameSubmitterAction(SameSubmitterActionEnum.ASSIGN_SUPERIOR.name());
        preview.setMultiApproverMode(mode.name());
        ReflectionTestUtils.invokeMethod(flowService, "previewNextApproverNodeWithExceptionHandler", instance,
                preview, "org-1");
        assertEquals(expected, preview.getApproverList());

        for (String approver : mode == MultiApproverModeEnum.ANY ? List.of("supervisor") : expected) {
            ApprovalTask pending = tasks.stream()
                    .filter(task -> "NODE003".equals(task.getNodeId()) && approver.equals(task.getApproverId())
                            && ApprovalStatus.APPROVING.name().equals(task.getStatus()))
                    .findFirst().orElseThrow();
            pending.setStatus(ApprovalStatus.APPROVED.name());
            process(pending);
        }
        assertEquals(expected, tasks.stream().filter(task -> "NODE003".equals(task.getNodeId()))
                .map(ApprovalTask::getApproverId).toList());
        assertCompleted();
    }

    @ParameterizedTest
    @EnumSource(MultiApproverModeEnum.class)
    void supervisorReplacementWithOnlyOneRecipientCreatesOneTaskAndUsesSingleApproverRules(
            MultiApproverModeEnum mode) {
        enterNodeWithSubmitter(mode, List.of("submitter", "supervisor"), "supervisor");
        doCallRealMethod().when(flowService).isCurrentNodeMultiApprover(anyString(), anyString(), anyString());

        List<ApprovalTask> targetTasks = tasks.stream().filter(task -> "NODE003".equals(task.getNodeId())).toList();
        assertEquals(1, targetTasks.size());
        assertEquals("supervisor", targetTasks.getFirst().getApproverId());
        assertFalse(flowService.isCurrentNodeMultiApprover("NODE003", "submitter", "org-1"));
        targetTasks.getFirst().setStatus(ApprovalStatus.APPROVED.name());
        process(targetTasks.getFirst());
        assertCompleted();
    }

    @SuppressWarnings("unchecked")
    private void enterNodeWithSubmitter(MultiApproverModeEnum mode, List<String> approvers, String supervisorId) {
        ApprovalTask first = firstApprovedTask(List.of("previous-approver"));
        ApprovalNodeApproverResponse automatic = node("AUTO001", MultiApproverModeEnum.ANY, List.of());
        automatic.setApprovalType(ApprovalTypeEnum.AUTO_PASS.name());
        ApprovalNodeApproverResponse target = node("NODE003", mode, approvers);
        target.setSameSubmitterAction(SameSubmitterActionEnum.ASSIGN_SUPERIOR.name());
        ApprovalNodeApprover config = nodeMapper.selectByPrimaryKey(target.getId());
        config.setSameSubmitterAction(SameSubmitterActionEnum.ASSIGN_SUPERIOR.name());
        config.setApproverType(ApproverTypeEnum.MEMBER.name());
        config.setApproverList(JSON.toJSONString(approvers));
        BaseMapper<OrganizationUser> organizationUsers = mock(BaseMapper.class);
        OrganizationUser submitter = new OrganizationUser();
        submitter.setSupervisorId(supervisorId);
        when(organizationUsers.selectOne(any())).thenReturn(submitter);
        ReflectionTestUtils.setField(flowService, "organizationUserMapper", organizationUsers);
        ReflectionTestUtils.setField(flowService, "approvalNodeApproverMapper", nodeMapper);
        // 后续取人真实走同人转交逻辑，仅替换与本用例无关的成员解析。
        doCallRealMethod().when(flowService).getCurrentNodeApproverList(any(ApprovalInstance.class), anyString(),
                anyString());
        doAnswer(invocation -> ((List<String>) invocation.getArgument(3)).stream().map(id -> {
            User user = new User();
            user.setId(id);
            return user;
        }).toList()).when(flowService).resolveApprovers(anyString(), anyString(), any(), anyList(), any());
        nextNodes.put("NODE001", automatic);
        nextNodes.put("AUTO001", target);

        process(first);
    }

    private ApprovalNodeApproverResponse node(String id, MultiApproverModeEnum mode, List<String> approvers) {
        ApprovalNodeApproverResponse node = new ApprovalNodeApproverResponse();
        node.setId(id);
        node.setNodeType(ApprovalNodeTypeEnum.APPROVER.name());
        node.setApprovalType(ApprovalTypeEnum.MANUAL.name());
        node.setApproverType(ApproverTypeEnum.MEMBER.name());
        node.setApproverList(approvers);
        node.setCcList(List.of());
        node.setMultiApproverMode(mode.name());
        node.setSameSubmitterAction(SameSubmitterActionEnum.ALLOW.name());
        nodes.put(id, node);
        ApprovalNodeApprover config = new ApprovalNodeApprover();
        config.setMultiApproverMode(mode.name());
        config.setSameSubmitterAction(SameSubmitterActionEnum.ALLOW.name());
        when(nodeMapper.selectByPrimaryKey(id)).thenReturn(config);
        return node;
    }

    private ApprovalTask firstApprovedTask(List<String> approvers) {
        node("NODE001", MultiApproverModeEnum.ALL, approvers);
        for (String approver : approvers) {
            ApprovalTask task = new ApprovalTask();
            task.setId("task-" + approver);
            task.setInstanceId(instance.getId());
            task.setNodeId("NODE001");
            task.setApproverId(approver);
            task.setNodeRound(1);
            task.setType(ApprovalTaskType.NL.name());
            task.setStatus(ApprovalStatus.APPROVED.name());
            tasks.add(task);
        }
        return tasks.getLast();
    }

    private void process(ApprovalTask task) {
        ReflectionTestUtils.invokeMethod(actionService, "approvedProcess", instance, task, task.getApproverId(),
                "org-1", "示例商机");
    }

    private void assertCompleted() {
        assertEquals("END001", instance.getCurrentNodeId());
        assertEquals(ApprovalStatus.APPROVED.name(), instance.getApprovalStatus());
        assertNotNull(instance.getApprovalTime());
        assertEquals(ApprovalStatus.APPROVED.name(), resourceStatus.get());
        verify(instanceMapper, atLeastOnce()).updateById(instance);
        verify(extInstanceMapper).updateApprovalStatus("opportunity", "opportunity-1", ApprovalStatus.APPROVED.name());
        verify(extInstanceMapper).setApproved("opportunity", "opportunity-1");
    }
}
