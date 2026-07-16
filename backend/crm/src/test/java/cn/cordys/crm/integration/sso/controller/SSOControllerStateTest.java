package cn.cordys.crm.integration.sso.controller;

import cn.cordys.crm.integration.sso.service.OAuthStateService;
import cn.cordys.crm.integration.sso.service.SSOService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SSO 回调入口的回归测试，确保缺少 state 时在授权码交换前终止请求。
 */
class SSOControllerStateTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/sso/callback/oauth/wecom",
            "/sso/callback/oauth/ding-talk",
            "/sso/callback/oauth/lark",
            "/sso/callback/oauth/lark-mobile",
            "/sso/callback/oauth/github"
    })
    void oauthCallbacksRejectCodeWithoutStateBeforeExchangingCode(String path) throws Exception {
        SSOService ssoService = mock(SSOService.class);
        OAuthStateService stateService = mock(OAuthStateService.class);
        SSOController controller = new SSOController();
        ReflectionTestUtils.setField(controller, "ssoService", ssoService);
        ReflectionTestUtils.setField(controller, "oauthStateService", stateService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get(path).param("code", "attacker-code"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stateService, ssoService);
    }
}
