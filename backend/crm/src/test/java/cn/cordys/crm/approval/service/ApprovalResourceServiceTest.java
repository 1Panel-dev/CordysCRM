package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ApprovalResourceServiceTest {

    private ApprovalResourceService service;
    private BaseMapper<ApprovalTask> taskMapper;
    private BaseMapper<ApprovalInstance> instanceMapper;
    private BaseMapper<ApprovalFlowVersion> flowVersionMapper;
    private ExtApprovalInstanceMapper extInstanceMapper;
    private ExtApprovalTaskMapper extTaskMapper;
    private ApprovalFlowService flowService;
    private ApprovalActionService actionService;
    private DataScopeService dataScopeService;
    private CustomFormDataService customFormDataService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalResourceService();
        taskMapper = mock(BaseMapper.class);
        instanceMapper = mock(BaseMapper.class);
        flowVersionMapper = mock(BaseMapper.class);
        extInstanceMapper = mock(ExtApprovalInstanceMapper.class);
        extTaskMapper = mock(ExtApprovalTaskMapper.class);
        flowService = mock(ApprovalFlowService.class);
        actionService = mock(ApprovalActionService.class);
        dataScopeService = mock(DataScopeService.class);
        customFormDataService = mock(CustomFormDataService.class);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "approvalFlowVersionMapper", flowVersionMapper);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", extInstanceMapper);
        ReflectionTestUtils.setField(service, "extApprovalTaskMapper", extTaskMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(service, "approvalActionService", actionService);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        ReflectionTestUtils.setField(service, "customFormDataService", customFormDataService);
    }

    @ParameterizedTest
    @EnumSource(ExecuteTimingEnum.class)
    void existingApprovingInstanceSkipsPushForEveryExecuteTiming(ExecuteTimingEnum timing) {
        ApprovalPushParam param = pushParam(timing);
        when(extInstanceMapper.existsApprovingInstance("org-a", "opportunity", "resource-a")).thenReturn(true);

        service.push(param);

        verify(extInstanceMapper).existsApprovingInstance("org-a", "opportunity", "resource-a");
        verifyNoMoreInteractions(extInstanceMapper);
        verifyNoInteractions(flowService, instanceMapper, taskMapper, actionService);
    }

    @Test
    void noApprovingInstanceContinuesToExistingFlowLookup() {
        ApprovalPushParam param = pushParam(ExecuteTimingEnum.UPDATE);
        when(extInstanceMapper.existsApprovingInstance("org-a", "opportunity", "resource-a")).thenReturn(false);
        IllegalStateException flowLookupReached = new IllegalStateException("已进入原审批流查询");
        when(flowService.getEnabledFlow("opportunity", "org-a")).thenThrow(flowLookupReached);

        assertSame(flowLookupReached, assertThrows(IllegalStateException.class, () -> service.push(param)));

        InOrder order = inOrder(extInstanceMapper, flowService);
        order.verify(extInstanceMapper).existsApprovingInstance("org-a", "opportunity", "resource-a");
        order.verify(flowService).getEnabledFlow("opportunity", "org-a");
        order.verifyNoMoreInteractions();
        verifyNoInteractions(instanceMapper, taskMapper, actionService);
    }

    @Test
    void approvingInstanceQueryScopesOrganizationFormAndResourceAcrossTimingsAndVersions() throws Exception {
        Configuration configuration = new Configuration();
        String resource = ExtApprovalInstanceMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = Map.of(
                "organizationId", "org-a", "formKey", "opportunity", "resourceId", "resource-a");
        BoundSql sql = configuration
                .getMappedStatement(ExtApprovalInstanceMapper.class.getName() + ".existsApprovingInstance")
                .getBoundSql(parameters);

        assertEquals("""
                SELECT EXISTS (
                    SELECT 1
                    FROM approval_instance ai
                    INNER JOIN approval_flow_version afv ON afv.id = ai.flow_version_id
                    WHERE afv.organization_id = ?
                      AND ai.type = ?
                      AND ai.resource_id = ?
                      AND ai.approval_status = 'APPROVING'
                )
                """.replaceAll("\\s+", " ").trim(), sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("organizationId", "formKey", "resourceId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals(List.of("org-a", "opportunity", "resource-a"),
                sql.getParameterMappings().stream().map(mapping -> parameters.get(mapping.getProperty())).toList());
    }

    private ApprovalPushParam pushParam(ExecuteTimingEnum timing) {
        return ApprovalPushParam.builder()
                .resourceId("resource-a")
                .formKey("opportunity")
                .orgId("org-a")
                .userId("user-a")
                .executeTimingEnum(timing)
                .build();
    }

    @Test
    void completedAddSignApproverCanViewFinishedApproval() {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-1");
        instance.setType("contract");
        instance.setSubmitterId("submitter");
        instance.setApprovalStatus(ApprovalStatus.APPROVED.name());

        ApprovalTask signTask = new ApprovalTask();
        signTask.setInstanceId(instance.getId());
        signTask.setApproverId("sign-approver");
        signTask.setType(ApprovalTaskType.SN.name());
        signTask.setStatus(ApprovalStatus.APPROVED.name());
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            return wrapper.getParams().containsValue(instance.getId())
                    && wrapper.getParams().containsValue(signTask.getApproverId()) ? List.of(signTask) : List.of();
        });

        boolean permitted = ReflectionTestUtils.invokeMethod(service, "hasApprovalTaskPermission", instance,
                "sign-approver");

        assertTrue(permitted);
    }

    @Test
    void instanceViewPermissionIncludesHistoricalAddSignTasks() {
        ApprovalInstance instance = instanceForView();
        ApprovalTask signTask = new ApprovalTask();
        signTask.setInstanceId(instance.getId());
        signTask.setApproverId("sign-approver");
        signTask.setType(ApprovalTaskType.SN.name());
        signTask.setStatus(ApprovalStatus.APPROVED.name());
        signTask.setNodeRound(-1);
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            assertEquals(2, wrapper.getParams().size());
            return wrapper.getParams().containsValue(instance.getId())
                    && wrapper.getParams().containsValue(signTask.getApproverId()) ? List.of(signTask) : List.of();
        });

        assertTrue(service.hasInstanceViewPermission(instance, "sign-approver", "org-a"));

        verifyNoInteractions(extTaskMapper, dataScopeService, customFormDataService);
    }

    @Test
    void instanceViewPermissionRejectsOtherOrganizationAndMissingFlowVersionBeforeSubmitter() {
        ApprovalInstance instance = instanceForView();
        ApprovalFlowVersion version = new ApprovalFlowVersion();
        version.setOrganizationId("org-b");
        when(flowVersionMapper.selectByPrimaryKey(instance.getFlowVersionId())).thenReturn(version);

        assertFalse(service.hasInstanceViewPermission(instance, instance.getSubmitterId(), "org-a"));

        when(flowVersionMapper.selectByPrimaryKey(instance.getFlowVersionId())).thenReturn(null);
        assertFalse(service.hasInstanceViewPermission(instance, instance.getSubmitterId(), "org-a"));
        verifyNoInteractions(taskMapper, dataScopeService, customFormDataService, extTaskMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void instanceBusinessPermissionRequiresReadAndOwnerDataScope(boolean dataPermitted) {
        ApprovalInstance instance = instanceForView();
        ResourceAccessContextProvider provider = mock(ResourceAccessContextProvider.class);
        when(provider.getFormType()).thenReturn("contract");
        when(provider.requiresOwner()).thenReturn(true);
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId("resource-owner");
        when(provider.getAccessContext("resource-a", "org-a")).thenReturn(context);
        when(dataScopeService.hasDataPermission("reader", "org-a", "resource-owner", PermissionConstants.CONTRACT_READ))
                .thenReturn(dataPermitted);

        try (MockedStatic<PermissionUtils> permissions = mockStatic(PermissionUtils.class,
                withSettings().mockMaker(MockMakers.INLINE));
                MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class,
                        withSettings().mockMaker(MockMakers.INLINE))) {
            permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CONTRACT_READ)).thenReturn(true);
            beans.when(() -> CommonBeanFactory.getBeansOfType(ResourceAccessContextProvider.class))
                    .thenReturn(Map.of("contract", provider));

            assertEquals(dataPermitted, service.hasInstanceViewPermission(instance, "reader", "org-a"));
        }
        verify(dataScopeService).hasDataPermission("reader", "org-a", "resource-owner",
                PermissionConstants.CONTRACT_READ);
        verifyNoInteractions(extTaskMapper, customFormDataService);
    }

    @Test
    void latestInstanceParticipantDoesNotGainPermissionOnOlderInstance() {
        ApprovalInstance instance = instanceForView();
        ApprovalInstance latest = new ApprovalInstance();
        latest.setId("instance-new");
        latest.setSubmitterId("new-submitter");
        when(extTaskMapper.selectLatestInstanceByResourceId(instance.getResourceId())).thenReturn(latest);

        try (MockedStatic<PermissionUtils> permissions = mockStatic(PermissionUtils.class,
                withSettings().mockMaker(MockMakers.INLINE))) {
            permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CONTRACT_READ)).thenReturn(false);
            assertFalse(service.hasInstanceViewPermission(instance, "new-submitter", "org-a"));
        }

        verifyNoInteractions(extTaskMapper, dataScopeService, customFormDataService);
    }

    @Test
    void instanceBusinessPermissionRejectsMissingProviderResourceAndOwner() {
        ApprovalInstance instance = instanceForView();
        ResourceAccessContextProvider provider = mock(ResourceAccessContextProvider.class);
        when(provider.getFormType()).thenReturn("contract");
        when(provider.requiresOwner()).thenReturn(true);
        try (MockedStatic<PermissionUtils> permissions = mockStatic(PermissionUtils.class,
                withSettings().mockMaker(MockMakers.INLINE));
                MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class,
                        withSettings().mockMaker(MockMakers.INLINE))) {
            permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CONTRACT_READ)).thenReturn(true);
            beans.when(() -> CommonBeanFactory.getBeansOfType(ResourceAccessContextProvider.class))
                    .thenReturn(Map.of());
            assertFalse(service.hasInstanceViewPermission(instance, "reader", "org-a"));

            beans.when(() -> CommonBeanFactory.getBeansOfType(ResourceAccessContextProvider.class))
                    .thenReturn(Map.of("contract", provider));
            assertFalse(service.hasInstanceViewPermission(instance, "reader", "org-a"));

            when(provider.getAccessContext("resource-a", "org-a")).thenReturn(new ResourceAccessContext());
            assertFalse(service.hasInstanceViewPermission(instance, "reader", "org-a"));
        }
        verifyNoInteractions(dataScopeService, customFormDataService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void customFormInstanceReusesCustomFormDataViewPermission(boolean permitted) {
        ApprovalInstance instance = instanceForView();
        instance.setType("custom-form-a");
        when(customFormDataService.hasViewPermission("resource-a", "reader", "org-a")).thenReturn(permitted);
        try (MockedStatic<PermissionUtils> permissions = mockStatic(PermissionUtils.class,
                withSettings().mockMaker(MockMakers.INLINE))) {
            permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOM_FORM_READ))
                    .thenReturn(true);
            assertEquals(permitted, service.hasInstanceViewPermission(instance, "reader", "org-a"));
        }
        verify(customFormDataService).hasViewPermission("resource-a", "reader", "org-a");
        verifyNoInteractions(extTaskMapper, dataScopeService);
    }

    @Test
    void historicalInstanceParticipantGainsViewPermissionWithoutResourceCheck() {
        when(extTaskMapper.existsResourceParticipant("resource-a", "history-approver")).thenReturn(true);

        Boolean permitted = ReflectionTestUtils.invokeMethod(service, "hasViewPermission",
                "resource-a", "history-approver", "org-a");

        assertTrue(permitted);
        verify(extTaskMapper).existsResourceParticipant("resource-a", "history-approver");
        verifyNoInteractions(dataScopeService, customFormDataService);
    }

    @Test
    void nonParticipantFallsBackToResourceViewPermission() {
        when(extTaskMapper.existsResourceParticipant("resource-a", "reader")).thenReturn(false);
        ResourceAccessContextProvider provider = mock(ResourceAccessContextProvider.class);
        when(provider.getFormType()).thenReturn("contract");
        when(provider.requiresOwner()).thenReturn(true);
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId("resource-owner");
        when(provider.getAccessContext("resource-a", "org-a")).thenReturn(context);
        when(dataScopeService.hasDataPermission("reader", "org-a", "resource-owner", PermissionConstants.CONTRACT_READ))
                .thenReturn(true);

        try (MockedStatic<PermissionUtils> permissions = mockStatic(PermissionUtils.class,
                withSettings().mockMaker(MockMakers.INLINE));
                MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class,
                        withSettings().mockMaker(MockMakers.INLINE))) {
            permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CONTRACT_READ)).thenReturn(true);
            beans.when(() -> CommonBeanFactory.getBeansOfType(ResourceAccessContextProvider.class))
                    .thenReturn(Map.of("contract", provider));

            // 泛型 invokeMethod 的返回值需先落到 Boolean, 否则 assertTrue 会重载到 BooleanSupplier
            Boolean permitted = ReflectionTestUtils.invokeMethod(service, "hasViewPermission",
                    "resource-a", "reader", "org-a");
            assertTrue(permitted);
        }
    }

    @Test
    void resourceParticipantQueryCoversSubmitterAndAnyHistoricalTask() throws Exception {
        Configuration configuration = new Configuration();
        String resource = ExtApprovalTaskMapper.class.getName().replace('.', '/') + ".xml";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "找不到 Mapper XML：" + resource);
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = Map.of("resourceId", "resource-a", "userId", "user-a");
        BoundSql sql = configuration
                .getMappedStatement(ExtApprovalTaskMapper.class.getName() + ".existsResourceParticipant")
                .getBoundSql(parameters);

        assertEquals("""
                SELECT EXISTS (
                    SELECT 1
                    FROM approval_instance ai
                    WHERE ai.resource_id = ?
                      AND (ai.submitter_id = ?
                           OR EXISTS (
                               SELECT 1
                               FROM approval_task at
                               WHERE at.instance_id = ai.id
                                 AND at.approver_id = ?
                           ))
                )
                """.replaceAll("\\s+", " ").trim(), sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("resourceId", "userId", "userId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
    }

    private ApprovalInstance instanceForView() {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-old");
        instance.setFlowVersionId("version-a");
        instance.setType("contract");
        instance.setResourceId("resource-a");
        instance.setSubmitterId("submitter");
        ApprovalFlowVersion version = new ApprovalFlowVersion();
        version.setId(instance.getFlowVersionId());
        version.setOrganizationId("org-a");
        when(flowVersionMapper.selectByPrimaryKey(instance.getFlowVersionId())).thenReturn(version);
        return instance;
    }
}
