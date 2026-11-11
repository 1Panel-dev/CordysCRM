package cn.cordys.crm.approval.service;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.dto.response.ApprovalFlowByFormTypeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalFlowServiceTest {

    private static MessageSource previousMessageSource;

    private ApprovalFlowService service;
    private BaseMapper<ApprovalFlow> flowMapper;
    private ExtApprovalInstanceMapper instanceMapper;
    private RoleService roleService;

    @BeforeAll
    static void initMessageSource() {
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        ReflectionTestUtils.setField(Translator.class, "messageSource", messages);
    }

    @AfterAll
    static void restoreMessageSource() {
        ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalFlowService();
        flowMapper = mock(BaseMapper.class);
        instanceMapper = mock(ExtApprovalInstanceMapper.class);
        roleService = mock(RoleService.class);
        ReflectionTestUtils.setField(service, "approvalFlowMapper", flowMapper);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "roleService", roleService);
        when(roleService.getPermissionSetting()).thenReturn(List.of());
    }

    @ParameterizedTest
    @CsvSource({ "true, true", "true, false", "false, true", "false, false" })
    void returnsFlowSwitchAndCurrentApprovingDataIndependently(boolean enabled, boolean hasApprovingData) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setId("flow-a");
        flow.setFormType("contract");
        flow.setEnable(enabled);
        when(flowMapper.select(any(ApprovalFlow.class))).thenReturn(List.of(flow));
        when(instanceMapper.existsApprovingResource("contract", "contract", "org-a")).thenReturn(hasApprovingData);

        ApprovalFlowByFormTypeResponse response = service.getByFormType("contract", "org-a");

        assertNotNull(response);
        assertEquals("flow-a", response.getId());
        assertEquals(enabled, response.getEnable());
        assertEquals(hasApprovingData, response.getExistApprovingData());
        ArgumentCaptor<ApprovalFlow> query = ArgumentCaptor.forClass(ApprovalFlow.class);
        verify(flowMapper).select(query.capture());
        assertEquals("contract", query.getValue().getFormType());
        assertEquals("org-a", query.getValue().getOrganizationId());
        assertEquals(false, query.getValue().getDeleted());
        assertNull(query.getValue().getEnable());
    }

    @Test
    void returnsNullWhenNoFlowIsConfigured() {
        when(flowMapper.select(any(ApprovalFlow.class))).thenReturn(List.of());

        assertNull(service.getByFormType("contract", "org-a"));

        verifyNoInteractions(instanceMapper, roleService);
    }

    @Test
    void queriesCustomFormByItsConcreteFormIdAndOrganization() {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFormType("custom-form-a");
        flow.setEnable(false);
        when(flowMapper.select(any(ApprovalFlow.class))).thenReturn(List.of(flow));
        when(instanceMapper.existsApprovingResource("custom_form_data", "custom-form-a", "org-b"))
                .thenReturn(true);

        ApprovalFlowByFormTypeResponse response = service.getByFormType("custom-form-a", "org-b");

        assertNotNull(response);
        assertEquals(true, response.getExistApprovingData());
        verify(instanceMapper).existsApprovingResource("custom_form_data", "custom-form-a", "org-b");
        ArgumentCaptor<ApprovalFlow> query = ArgumentCaptor.forClass(ApprovalFlow.class);
        verify(flowMapper).select(query.capture());
        assertEquals("custom-form-a", query.getValue().getFormType());
        assertEquals("org-b", query.getValue().getOrganizationId());
    }

    @ParameterizedTest
    @CsvSource({ "true, true, PENDING", "true, false, NONE", "true, , NONE", "false, true, NONE" })
    void initialStatusRequiresAnEnabledFlowThatExecutesOnCreate(boolean hasEnabledFlow, Boolean createExecute,
            String expectedStatus) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setCreateExecute(createExecute);
        ApprovalFlowService flowService = spy(service);
        doReturn(hasEnabledFlow ? flow : null).when(flowService).getEnabledFlow("custom-form-a", "org-b");

        assertEquals(expectedStatus, flowService.getInitialApprovalStatus("custom-form-a", "org-b"));

        verify(flowService).getEnabledFlow("custom-form-a", "org-b");
    }
}
