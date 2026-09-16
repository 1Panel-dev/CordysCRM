package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.OpportunityStageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityStageResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.system.service.StageAdvancedConfigService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpportunityStagePermissionTest {
    private final OpportunityService service = new OpportunityService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Opportunity> mapper = mock(BaseMapper.class);
    private final DataScopeService dataScope = mock(DataScopeService.class);
    private final ExtOpportunityStageConfigMapper stages = mock(ExtOpportunityStageConfigMapper.class);
    private final ExtOpportunityMapper extMapper = mock(ExtOpportunityMapper.class);
    private final StageAdvancedConfigService stageAdvancedConfigService = mock(StageAdvancedConfigService.class);
    private final Opportunity opportunity = new Opportunity();
    private final OpportunityStageRequest request = new OpportunityStageRequest();
    private MockedStatic<SessionUtils> session;
    private MockedStatic<PermissionUtils> permissions;
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "opportunityMapper", mapper);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, "extOpportunityStageConfigMapper", stages);
        ReflectionTestUtils.setField(service, "extOpportunityMapper", extMapper);
        ReflectionTestUtils.setField(service, "stageAdvancedConfigService", stageAdvancedConfigService);
        session = mockStatic(SessionUtils.class);
        permissions = mockStatic(PermissionUtils.class);
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        session.when(SessionUtils::getUserId).thenReturn("sales-user");
        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE))
                .thenReturn(true);
        opportunity.setId("test-opportunity");
        opportunity.setOrganizationId("test-org");
        opportunity.setOwner("sales-user");
        opportunity.setStage("CREATE");
        request.setId(opportunity.getId());
        request.setStage("SUCCESS");
        when(mapper.selectByPrimaryKey(request.getId())).thenReturn(opportunity);
        when(dataScope.hasDataPermission("sales-user", "test-org", "sales-user",
                PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)).thenReturn(true);
        OpportunityStageResponse success = new OpportunityStageResponse();
        success.setId("SUCCESS");
        success.setName("Success");
        success.setType("END");
        success.setRate("100");
        when(stages.getStageConfigList("test-org")).thenReturn(List.of(success));
        when(stageAdvancedConfigService.checkStage("CREATE", "SUCCESS", FormKey.OPPORTUNITY.getKey()))
                .thenReturn(true);
        OperationLogContext.putEmptySpan();
    }

    @AfterEach
    void tearDown() {
        OperationLogContext.clear();
        translator.close();
        permissions.close();
        session.close();
    }

    @Test
    void selfScopeCannotUpdateAnotherOwnersOpportunity() {
        opportunity.setOwner("other-sales-user");
        assertThrows(GenericException.class, () -> service.updateStage(request, "test-org"));
        verify(mapper, never()).update(any(Opportunity.class));
        verifyNoInteractions(stages, extMapper);
    }

    @Test
    void crossOrganizationIsDeniedEvenForAnAllowedOwner() {
        opportunity.setOrganizationId("other-org");
        assertThrows(GenericException.class, () -> service.updateStage(request, "test-org"));
        verify(mapper, never()).update(any(Opportunity.class));
        verifyNoInteractions(dataScope, stages, extMapper);
    }

    @Test
    void anonymousCallerIsDeniedBeforeLoadingOpportunity() {
        session.when(SessionUtils::getUserId).thenReturn(null);
        assertThrows(GenericException.class, () -> service.updateStage(request, "test-org"));
        verifyNoInteractions(mapper, dataScope, stages, extMapper);
    }

    @Test
    void resignPermissionAloneDoesNotGrantUpdateAccess() {
        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE))
                .thenReturn(false);
        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_RESIGN))
                .thenReturn(true);
        assertThrows(GenericException.class, () -> service.updateStage(request, "test-org"));
        verifyNoInteractions(mapper, dataScope, stages, extMapper);
    }

    @Test
    void unknownStageIsRejectedBeforeWriting() {
        request.setStage("unknown-stage");
        assertThrows(GenericException.class, () -> service.updateStage(request, "test-org"));
        verify(mapper, never()).update(any(Opportunity.class));
        verifyNoInteractions(extMapper);
    }

    @Test
    void ownerCanWinOpportunity() {
        service.updateStage(request, "test-org");
        ArgumentCaptor<Opportunity> saved = ArgumentCaptor.forClass(Opportunity.class);
        verify(mapper).update(saved.capture());
        assertEquals("SUCCESS", saved.getValue().getStage());
        assertEquals("CREATE", saved.getValue().getLastStage());
        assertNotNull(saved.getValue().getActualEndTime());
        assertNotNull(OperationLogContext.getVariable(OperationLogContext.OPERATION_LOG_CONTEXT_KEY));
    }

    @Test
    void disallowedTransitionDoesNotWriteOpportunity() {
        when(stageAdvancedConfigService.checkStage("CREATE", "SUCCESS", FormKey.OPPORTUNITY.getKey()))
                .thenReturn(false);
        service.updateStage(request, "test-org");
        verify(mapper, never()).update(any(Opportunity.class));
        verifyNoInteractions(extMapper);
    }

    @Test
    void dataScopeCanAuthorizeAnotherOwner() {
        opportunity.setOwner("team-member");
        when(dataScope.hasDataPermission("sales-user", "test-org", "team-member",
                PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)).thenReturn(true);
        service.updateStage(request, "test-org");
        verify(mapper).update(any(Opportunity.class));
    }
}
