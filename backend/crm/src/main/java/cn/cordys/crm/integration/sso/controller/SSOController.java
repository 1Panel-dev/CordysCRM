package cn.cordys.crm.integration.sso.controller;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.utils.IpUtils;
import cn.cordys.crm.integration.sso.service.SSOService;
import cn.cordys.security.SessionUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping(value = "/wecom")
    @Operation(summary = "获取企业微信登陆验证")
    public SessionUser callbackWeCom(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeWeComCode(code, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping("/oauth/wecom")
    public SessionUser callbackWeComOauth(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeWeComOauth2(code, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping(value = "/ding-talk")
    @Operation(summary = "获取钉钉扫码登陆验证")
    public SessionUser callbackDingTalk(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeDingTalkCode(code, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping("/oauth/ding-talk")
    public SessionUser callbackDingTalkOauth(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeDingTalkOauth2(code, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping(value = "/lark")
    @Operation(summary = "获取飞书扫码登陆验证")
    public SessionUser callbackLark(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeLarkCode(code, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping("/oauth/lark")
    public SessionUser callbackLarkOauth(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeLarkOauth2(code, false, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping("/oauth/lark-mobile")
    public SessionUser callbackLarkOauthByMobile(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        return ssoService.exchangeLarkOauth2(code, true, IpUtils.getClientIpAddress(httpServletRequest));
    }

    @GetMapping("/oauth/github")
    public ModelAndView callbackOauth(@RequestParam("code") String code, HttpServletRequest httpServletRequest) {
        SessionUser sessionUser = ssoService.exchangeGitOauth2(code, IpUtils.getClientIpAddress(httpServletRequest));
        return new ModelAndView("redirect:/#/?_token=" + CodingUtils.base64Encoding(sessionUser.getSessionId()) + "&_csrf=" + sessionUser.getCsrfToken());
    }
}
