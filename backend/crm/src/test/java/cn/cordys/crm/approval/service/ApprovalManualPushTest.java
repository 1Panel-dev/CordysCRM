package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
import cn.cordys.crm.approval.dto.ApprovalResourceBatchParam;
import cn.cordys.crm.approval.dto.ApprovalResourceState;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeExceptionResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalManualPushTest {

    private static ApplicationContext previousContext;
    private static MessageSource previousMessageSource;

    private ApprovalResourceService service;
    private ExtApprovalInstanceMapper extMapper;
    private BaseMapper<ApprovalInstance> instanceMapper;
    private BaseMapper<ApprovalFlowVersion> versionMapper;
    private ApprovalFlowService flowService;
    private ApprovalActionService actionService;
    private ApprovalResourceBaseParam param;
    private ApprovalResourceState state;
    private ApprovalFlow flow;

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
        service = spy(new ApprovalResourceService());
        extMapper = mock(ExtApprovalInstanceMapper.class);
        instanceMapper = mock(BaseMapper.class);
        versionMapper = mock(BaseMapper.class);
        flowService = mock(ApprovalFlowService.class);
        actionService = mock(ApprovalActionService.class);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", extMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "approvalFlowVersionMapper", versionMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(service, "approvalActionService", actionService);
        doNothing().when(service).updateResourceApprovalStatus(any(), any(), any(), any(), any());
        param = new ApprovalResourceBaseParam();
        param.setResourceId("resource-a");
        param.setFormKey("contract");
        state = new ApprovalResourceState();
        state.setApprovalStatus("PENDING");
        state.setApproved(false);
        state.setCreateUser("creator");
        state.setOwner("owner");
        when(extMapper.selectResourceForUpdate("contract", "resource-a", "org-a", "contract")).thenReturn(state);
        flow = new ApprovalFlow();
        flow.setId("flow-a");
        flow.setCurrentVersionId("version-a");
        flow.setCreateExecute(true);
        when(flowService.getEnabledFlow("contract", "org-a")).thenReturn(flow);
        ApprovalNodeApproverResponse node = new ApprovalNodeApproverResponse();
        node.setId("node-a");
        node.setNodeType("APPROVER");
        when(flowService.getResourceApprovalInstanceFirstNode(any(), eq("org-a"))).thenReturn(node);
    }

    @ParameterizedTest
    @ValueSource(strings = { "NULL_PARAM", "RESOURCE", "FORM", "USER", "ORG" })
    void manualPushRejectsMissingIdentifiersBeforeReadingOrWriting(String missing) {
        String userId = "USER".equals(missing) ? " " : "creator";
        String orgId = "ORG".equals(missing) ? " " : "org-a";
        if ("RESOURCE".equals(missing)) {
            param.setResourceId(" ");
        } else if ("FORM".equals(missing)) {
            param.setFormKey(" ");
        }

        assertThrows(GenericException.class,
                () -> service.manualPush("NULL_PARAM".equals(missing) ? null : param, userId, orgId));

        verifyNoInteractions(extMapper, flowService, instanceMapper, actionService);
    }

    @ParameterizedTest
    @CsvSource({ "contract,contract", "clue,clue", "customer,customer", "opportunity,opportunity",
            "quotation,opportunity_quotation", "invoice,contract_invoice", "order,sales_order",
            "custom-form-a,custom_form_data" })
    void manualPushScopesResourceLockToActualFormAndOrganization(String formKey, String tableName) {
        param.setFormKey(formKey);
        when(flowService.getEnabledFlow(formKey, "other-org")).thenReturn(flow);
        when(extMapper.selectResourceForUpdate(tableName, "resource-a", "other-org", formKey)).thenReturn(null);

        assertThrows(GenericException.class, () -> service.manualPush(param, "creator", "other-org"));

        verify(extMapper).selectResourceForUpdate(tableName, "resource-a", "other-org", formKey);
        assertNoWrites();
        verifyNoMoreFlowLookups(formKey, "other-org");
    }

    @Test
    void manualPushRejectsCustomResourceThatDoesNotBelongToRequestedForm() {
        param.setFormKey("other-custom-form");
        when(flowService.getEnabledFlow("other-custom-form", "org-a")).thenReturn(flow);

        assertThrows(GenericException.class, () -> service.manualPush(param, "creator", "org-a"));

        verify(extMapper).selectResourceForUpdate("custom_form_data", "resource-a", "org-a", "other-custom-form");
        assertNoWrites();
        verifyNoMoreFlowLookups("other-custom-form", "org-a");
    }

    @ParameterizedTest
    @ValueSource(strings = { "PENDING", "APPROVING" })
    void manualPushRejectsUnrelatedUserBeforeNewOrRepeatedSubmission(String approvalStatus) {
        state.setApprovalStatus(approvalStatus);

        assertThrows(GenericException.class, () -> service.manualPush(param, "unrelated-user", "org-a"));

        assertNoWrites();
        verifyNoMoreFlowLookups("contract", "org-a");
    }

    @ParameterizedTest
    @CsvSource({ "creator,PENDING", "owner,PENDING", "creator,UNAPPROVED", "owner,UNAPPROVED",
            "creator,REVOKED", "owner,REVOKED" })
    void creatorAndOwnerMaySubmitOrResubmitCreateApproval(String userId, String approvalStatus) {
        state.setApprovalStatus(approvalStatus);

        assertFalse(service.manualPush(param, userId, "org-a").isResubmitNodeInvalid());

        verifyInsertedInstance("contract", userId, ExecuteTimingEnum.CREATE);
    }

    @Test
    void creatorMaySubmitPendingResourceWithNoHistoricalApprovalFlag() {
        state.setApproved(null);

        service.manualPush(param, "creator", "org-a");

        verifyInsertedInstance("contract", "creator", ExecuteTimingEnum.CREATE);
    }

    @ParameterizedTest
    @CsvSource({ "customer,customer", "clue,clue", "quotation,opportunity_quotation",
            "custom-form-a,custom_form_data" })
    void creatorCanSubmitResourceWithoutOwner(String formKey, String tableName) {
        param.setFormKey(formKey);
        state.setOwner(null);
        when(extMapper.selectResourceForUpdate(tableName, "resource-a", "org-a", formKey)).thenReturn(state);
        when(flowService.getEnabledFlow(formKey, "org-a")).thenReturn(flow);

        service.manualPush(param, "creator", "org-a");

        verifyInsertedInstance(formKey, "creator", ExecuteTimingEnum.CREATE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "creator", "owner" })
    void authorizedRepeatedManualSubmissionIsIdempotentEvenAfterConfigurationChanges(String userId) {
        state.setApprovalStatus("APPROVING");
        state.setApproved(true);
        flow.setCreateExecute(false);

        assertFalse(service.manualPush(param, userId, "org-a").isResubmitNodeInvalid());

        verify(extMapper).selectResourceForUpdate("contract", "resource-a", "org-a", "contract");
        assertNoWrites();
        verifyNoMoreFlowLookups("contract", "org-a");
    }

    @ParameterizedTest
    @ValueSource(strings = { "NONE", "APPROVED", "AUTO_APPROVED", "AUTO_UNAPPROVED", "UNKNOWN" })
    void manualPushRejectsInvalidResourceStatus(String approvalStatus) {
        state.setApprovalStatus(approvalStatus);

        assertThrows(GenericException.class, () -> service.manualPush(param, "creator", "org-a"));

        assertNoWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = { "PENDING", "UNAPPROVED", "REVOKED" })
    void manualPushCannotTreatPreviouslyApprovedResourceAsCreate(String approvalStatus) {
        state.setApprovalStatus(approvalStatus);
        state.setApproved(true);

        assertThrows(GenericException.class, () -> service.manualPush(param, "creator", "org-a"));

        assertNoWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = { "MISSING_FLOW", "CREATE_DISABLED", "CREATE_NULL" })
    void manualPushRequiresEnabledCreateApprovalConfiguration(String scenario) {
        if ("MISSING_FLOW".equals(scenario)) {
            when(flowService.getEnabledFlow("contract", "org-a")).thenReturn(null);
        } else {
            flow.setCreateExecute("CREATE_DISABLED".equals(scenario) ? false : null);
            flow.setUpdateExecute(true);
            flow.setDeleteExecute(true);
        }

        assertThrows(GenericException.class, () -> service.manualPush(param, "creator", "org-a"));

        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = ExecuteTimingEnum.class, names = { "UPDATE", "DELETE" })
    void automaticPushKeepsItsTimingWithoutManualIdentityOrCreateRestrictions(ExecuteTimingEnum timing) {
        state.setApprovalStatus("APPROVED");
        state.setApproved(true);
        flow.setCreateExecute(false);
        flow.setUpdateExecute(true);
        flow.setDeleteExecute(true);
        ApprovalPushParam automatic = ApprovalPushParam.builder().resourceId("resource-a").formKey("contract")
                .orgId("org-a").userId("automatic-operator").executeTimingEnum(timing).build();

        service.push(automatic);

        verifyInsertedInstance("contract", "automatic-operator", timing);
        verify(service, never()).manualPush(any(), any(), any());
    }

    @Test
    void rejectedInstanceResubmitsAtSameNumberInNewVersionWithoutCopyingItsTasks() {
        ApprovalInstance previous = rejectedInstance();
        previous.setCurrentNodeId("old-node-id");
        ApprovalNodeApproverResponse target = new ApprovalNodeApproverResponse();
        target.setId("new-node-id");
        target.setNodeType("APPROVER");
        when(flowService.getResourceApprovalInstanceResubmitNode(any(), eq("P" + "N052"), eq("org-a")))
                .thenReturn(target);

        assertFalse(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        ArgumentCaptor<ApprovalInstance> inserted = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceMapper).insert(inserted.capture());
        ApprovalInstance current = inserted.getValue();
        assertNotEquals(previous.getId(), current.getId());
        assertEquals("version-a", current.getFlowVersionId());
        assertEquals("new-node-id", current.getCurrentNodeId());
        assertEquals("P" + "N052", current.getResubmitStartNodeNumber());
        verify(flowService, never()).getResourceApprovalInstanceFirstNode(any(), any());
        verify(actionService).handlerNextNodeApproverTasks(target, current, "creator", null, "org-a");
    }

    @Test
    void missingResubmitNodeFallsBackToBeginning() {
        rejectedInstance();

        assertTrue(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        verifyInsertedInstance("contract", "creator", ExecuteTimingEnum.CREATE);
        ArgumentCaptor<ApprovalInstance> inserted = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceMapper).insert(inserted.capture());
        assertNull(inserted.getValue().getResubmitStartNodeNumber());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void wholeResourceResubmissionDoesNotReportInvalidNode(String nodeNumber) {
        rejectedInstance().setResubmitStartNodeNumber(nodeNumber);

        assertFalse(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        verifyInsertedInstance("contract", "creator", ExecuteTimingEnum.CREATE);
        verify(flowService, never()).getResourceApprovalInstanceResubmitNode(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({ "EXCEPTION,UNAPPROVED", "END,APPROVED" })
    void missingResubmitNodeStillReportsFallbackWhenApprovalFinishesImmediately(String nodeType, String status) {
        rejectedInstance();
        ApprovalNodeResponse firstNode = new ApprovalNodeResponse();
        firstNode.setId("first-node");
        firstNode.setNodeType(nodeType);
        when(flowService.getResourceApprovalInstanceFirstNode(any(), eq("org-a"))).thenReturn(firstNode);
        doReturn("示例合同").when(service).getInstanceResourceName(any(), eq("resource-a"));

        assertTrue(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        ArgumentCaptor<ApprovalInstance> inserted = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceMapper).insert(inserted.capture());
        ApprovalInstance current = inserted.getValue();
        assertEquals(status, current.getApprovalStatus());
        assertEquals("first-node", current.getCurrentNodeId());
        assertNull(current.getResubmitStartNodeNumber());
        verify(actionService).sendFinishNotice(current, "示例合同", "ADMIN", "org-a");
        verify(actionService, never()).handlerNextNodeApproverTasks(any(), any(), any(), any(), any());
    }

    @Test
    void automaticRejectionDoesNotReuseThisRoundsStartAsNextResubmitChoice() {
        rejectedInstance();
        ApprovalNodeExceptionResponse target = new ApprovalNodeExceptionResponse();
        target.setId("automatic-reject-node");
        target.setNodeType("EXCEPTION");
        when(flowService.getResourceApprovalInstanceResubmitNode(any(), eq("P" + "N052"), eq("org-a")))
                .thenReturn(target);
        doReturn("示例合同").when(service).getInstanceResourceName(any(), eq("resource-a"));

        assertFalse(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        ArgumentCaptor<ApprovalInstance> inserted = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceMapper).insert(inserted.capture());
        ApprovalInstance current = inserted.getValue();
        assertEquals("UNAPPROVED", current.getApprovalStatus());
        assertEquals("automatic-reject-node", current.getCurrentNodeId());
        assertNull(current.getResubmitStartNodeNumber());
        verify(actionService).sendFinishNotice(current, "示例合同", "ADMIN", "org-a");
        verify(flowService, never()).getResourceApprovalInstanceFirstNode(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "APPROVED", "REVOKED" })
    void resubmitChoiceIsOnlyAppliedAfterRejection(String status) {
        ApprovalInstance previous = rejectedInstance();
        previous.setApprovalStatus(status);

        assertFalse(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        verifyInsertedInstance("contract", "creator", ExecuteTimingEnum.CREATE);
        verify(flowService, never()).getResourceApprovalInstanceResubmitNode(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({ "invoice,CREATE", "contract,UPDATE", "contract,DELETE" })
    void resubmitChoiceDoesNotCrossFormOrExecutionTiming(String formType, String timing) {
        ApprovalInstance previous = rejectedInstance();
        previous.setType(formType);
        previous.setExecuteTime(timing);

        assertFalse(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        verifyInsertedInstance("contract", "creator", ExecuteTimingEnum.CREATE);
        verify(flowService, never()).getResourceApprovalInstanceResubmitNode(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deletedOrReplacedFlowDoesNotReuseAnotherFlowsNodeNumber(boolean deleted) {
        rejectedInstance();
        ApprovalFlowVersion previousVersion = new ApprovalFlowVersion();
        previousVersion.setFlowId("another-flow");
        when(versionMapper.selectByPrimaryKey("old-version")).thenReturn(deleted ? null : previousVersion);

        assertTrue(service.manualPush(param, "creator", "org-a").isResubmitNodeInvalid());

        verifyInsertedInstance("contract", "creator", ExecuteTimingEnum.CREATE);
        verify(flowService, never()).getResourceApprovalInstanceResubmitNode(any(), any(), any());
    }

    private ApprovalInstance rejectedInstance() {
        state.setApprovalStatus("UNAPPROVED");
        ApprovalInstance previous = new ApprovalInstance();
        previous.setId("previous-instance");
        previous.setFlowVersionId("old-version");
        previous.setType("contract");
        previous.setExecuteTime("CREATE");
        previous.setApprovalStatus("UNAPPROVED");
        previous.setResubmitStartNodeNumber("P" + "N052");
        when(extMapper.selectLatestByResourceIdForUpdate("resource-a", "contract", "org-a")).thenReturn(previous);
        ApprovalFlowVersion previousVersion = new ApprovalFlowVersion();
        previousVersion.setFlowId("flow-a");
        when(versionMapper.selectByPrimaryKey("old-version")).thenReturn(previousVersion);
        return previous;
    }

    @Test
    void batchManualPushSubmitsEachEligibleResource() {
        ApprovalResourceState stateB = new ApprovalResourceState();
        stateB.setApprovalStatus("PENDING");
        stateB.setApproved(false);
        stateB.setCreateUser("creator");
        stateB.setOwner("owner");
        when(extMapper.selectResourceForUpdate("contract", "resource-b", "org-a", "contract")).thenReturn(stateB);

        ApprovalResourceBatchParam batchParam = new ApprovalResourceBatchParam();
        batchParam.setFormKey("contract");
        batchParam.setResourceIds(List.of("resource-a", "resource-b"));

        service.batchManualPush(batchParam, "creator", "org-a");

        ArgumentCaptor<ApprovalInstance> inserted = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceMapper, times(2)).insert(inserted.capture());
        assertEquals(List.of("resource-a", "resource-b"),
                inserted.getAllValues().stream().map(ApprovalInstance::getResourceId).toList());
    }

    @Test
    void batchManualPushRejectsEmptyResourceIdsBeforeAnyReadOrWrite() {
        ApprovalResourceBatchParam batchParam = new ApprovalResourceBatchParam();
        batchParam.setFormKey("contract");
        batchParam.setResourceIds(Collections.emptyList());

        assertThrows(GenericException.class, () -> service.batchManualPush(batchParam, "creator", "org-a"));

        verifyNoInteractions(extMapper, flowService, instanceMapper, actionService);
    }

    @Test
    void batchManualPushRejectsIneligibleResourceWithoutSubmittingTheRest() {
        state.setApprovalStatus("APPROVED");

        ApprovalResourceBatchParam batchParam = new ApprovalResourceBatchParam();
        batchParam.setFormKey("contract");
        batchParam.setResourceIds(List.of("resource-a", "resource-b"));

        assertThrows(GenericException.class, () -> service.batchManualPush(batchParam, "creator", "org-a"));

        assertNoWrites();
    }

    private void verifyInsertedInstance(String formKey, String userId, ExecuteTimingEnum timing) {
        ArgumentCaptor<ApprovalInstance> inserted = ArgumentCaptor.forClass(ApprovalInstance.class);
        verify(instanceMapper).insert(inserted.capture());
        ApprovalInstance instance = inserted.getValue();
        assertEquals("resource-a", instance.getResourceId());
        assertEquals(formKey, instance.getType());
        assertEquals("version-a", instance.getFlowVersionId());
        assertEquals(userId, instance.getSubmitterId());
        assertEquals(timing.name(), instance.getExecuteTime());
        assertEquals("APPROVING", instance.getApprovalStatus());
        assertEquals("node-a", instance.getCurrentNodeId());
        verify(service).updateResourceApprovalStatus(any(), eq("resource-a"), eq("APPROVING"), eq(userId), eq("org-a"));
        verify(actionService).handlerNextNodeApproverTasks(any(), eq(instance), eq(userId), eq(null), eq("org-a"));
    }

    private void assertNoWrites() {
        verifyNoInteractions(instanceMapper, actionService);
        verify(service, never()).updateResourceApprovalStatus(any(), any(), any(), any(), any());
    }

    /**
     * 提审配置查询（getEnabledFlow）已前移到资源锁之前, 故仅校验未继续解析审批节点。
     */
    private void verifyNoMoreFlowLookups(String formKey, String orgId) {
        verify(flowService).getEnabledFlow(formKey, orgId);
        verify(flowService, never()).getResourceApprovalInstanceFirstNode(any(), any());
        verify(flowService, never()).getResourceApprovalInstanceResubmitNode(any(), any(), any());
    }
}
