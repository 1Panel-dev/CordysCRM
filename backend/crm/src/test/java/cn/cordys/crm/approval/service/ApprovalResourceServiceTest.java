package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ApprovalResourceServiceTest {

    private ApprovalResourceService service;
    private BaseMapper<ApprovalTask> taskMapper;
    private BaseMapper<ApprovalInstance> instanceMapper;
    private ExtApprovalInstanceMapper extInstanceMapper;
    private ApprovalFlowService flowService;
    private ApprovalActionService actionService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalResourceService();
        taskMapper = mock(BaseMapper.class);
        instanceMapper = mock(BaseMapper.class);
        extInstanceMapper = mock(ExtApprovalInstanceMapper.class);
        flowService = mock(ApprovalFlowService.class);
        actionService = mock(ApprovalActionService.class);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", extInstanceMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", flowService);
        ReflectionTestUtils.setField(service, "approvalActionService", actionService);
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
        BoundSql sql = configuration.getMappedStatement(ExtApprovalInstanceMapper.class.getName() + ".existsApprovingInstance")
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

        boolean permitted = ReflectionTestUtils.invokeMethod(service, "hasApprovalTaskPermission", instance, "sign-approver");

        assertTrue(permitted);
    }
}
