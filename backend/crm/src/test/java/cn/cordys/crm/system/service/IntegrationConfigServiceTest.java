package cn.cordys.crm.system.service;

import cn.cordys.common.schedule.ScheduleService;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.DeThirdConfigRequest;
import cn.cordys.crm.integration.common.request.WecomThirdConfigRequest;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class IntegrationConfigServiceTest {
    private final IntegrationConfigService service = new IntegrationConfigService();
    private final TokenService tokenService = mock(TokenService.class);
    private final ExtOrganizationConfigMapper configMapper = mock(ExtOrganizationConfigMapper.class);
    private final ExtOrganizationConfigDetailMapper detailMapper = mock(ExtOrganizationConfigDetailMapper.class);
    private final BaseMapper<OrganizationConfig> configBaseMapper = mock(BaseMapper.class);
    private final BaseMapper<OrganizationConfigDetail> detailBaseMapper = mock(BaseMapper.class);
    private final ScheduleService scheduleService = mock(ScheduleService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "extOrganizationConfigMapper", configMapper);
        ReflectionTestUtils.setField(service, "extOrganizationConfigDetailMapper", detailMapper);
        ReflectionTestUtils.setField(service, "organizationConfigBaseMapper", configBaseMapper);
        ReflectionTestUtils.setField(service, "organizationConfigDetailBaseMapper", detailBaseMapper);
        ReflectionTestUtils.setField(service, "scheduleService", scheduleService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void dataEaseConnectionTestDoesNotSaveConfiguration(boolean connected) {
        DeThirdConfigRequest config = new DeThirdConfigRequest();
        config.setRedirectUrl("https://dataease.example");
        config.setDeBoardEnable(false);
        ThirdConfigBaseDTO<DeThirdConfigRequest> request = new ThirdConfigBaseDTO<>();
        request.setType("DE");
        request.setVerify(false);
        request.setConfig(config);
        when(tokenService.pingDeUrl(config.getRedirectUrl())).thenReturn(connected);

        assertEquals(connected, service.testConnection(request));

        verify(tokenService).pingDeUrl(config.getRedirectUrl());
        verifyNoInteractions(configMapper, detailMapper, configBaseMapper, detailBaseMapper, scheduleService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void weComConnectionTestChecksAgentWithoutSavingConfiguration(boolean available) {
        WecomThirdConfigRequest config = new WecomThirdConfigRequest();
        config.setCorpId("synthetic-corp");
        config.setAgentId("synthetic-agent");
        config.setAppSecret("synthetic-secret");
        ThirdConfigBaseDTO<WecomThirdConfigRequest> request = new ThirdConfigBaseDTO<>();
        request.setType("WECOM");
        request.setConfig(config);
        when(tokenService.getAssessToken(config.getCorpId(), config.getAppSecret())).thenReturn("synthetic-token");
        when(tokenService.checkWeComAgentAvailable("synthetic-token", config.getAgentId())).thenReturn(available);

        assertEquals(available, service.testConnection(request));

        verify(tokenService).getAssessToken(config.getCorpId(), config.getAppSecret());
        verify(tokenService).checkWeComAgentAvailable("synthetic-token", config.getAgentId());
        verifyNoInteractions(configMapper, detailMapper, configBaseMapper, detailBaseMapper, scheduleService);
    }
}
