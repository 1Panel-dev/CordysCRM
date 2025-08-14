package io.cordys.crm.integration.sso.controller;

import io.cordys.common.util.CodingUtils;
import io.cordys.crm.integration.sso.service.SSOService;
import io.cordys.security.SessionUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;


@RestController
@RequestMapping("/sso/callback")
public class SSOController {
    @Resource
    private SSOService ssoService;

    @GetMapping(value = "/we_com")
    @Operation(summary = "获取企业微信登陆验证")
    public SessionUser callbackWeCom(@RequestParam("code") String code) {
        return ssoService.exchangeWeComCode(code);
    }

    @GetMapping(value = "/ding_talk")
    @Operation(summary = "获取钉钉扫码登陆验证")
    public SessionUser callbackDingTalk(@RequestParam("code") String code) {
        return ssoService.exchangeDingTalkCode(code);
    }

    @GetMapping("/oauth/github")
    public ModelAndView callbackOauth(@RequestParam("code") String code) {
        SessionUser sessionUser = ssoService.exchangeGitOauth2(code);
        return new ModelAndView("redirect:/#/?_token=" + CodingUtils.base64Encoding(sessionUser.getSessionId()) + "&_csrf=" + sessionUser.getCsrfToken());
    }

    @GetMapping("/oauth/wecom")
    public SessionUser callbackWeComOauth(@RequestParam("code") String code) {
        return ssoService.exchangeWeComOauth2(code);
       // return new ModelAndView("redirect:/#/?_token=" + CodingUtils.base64Encoding(sessionUser.getSessionId()) + "&_csrf=" + sessionUser.getCsrfToken());
    }
}
