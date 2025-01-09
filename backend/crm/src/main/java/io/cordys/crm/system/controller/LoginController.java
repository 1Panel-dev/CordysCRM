package io.cordys.crm.system.controller;

import io.cordys.common.constants.UserSource;
import io.cordys.common.exception.GenericException;
import io.cordys.common.request.LoginRequest;
import io.cordys.common.response.result.CrmHttpResultCode;
import io.cordys.common.util.Translator;
import io.cordys.common.util.rsa.RsaKey;
import io.cordys.common.util.rsa.RsaUtils;
import io.cordys.crm.system.service.UserLoginService;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import io.cordys.security.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器，负责处理用户登录、校验和退出操作。
 * <p>
 * 该控制器包含检查是否已登录、获取公钥、用户登录和退出登录功能。
 * </p>
 */
@RestController
@RequestMapping
@Tag(name = "登录")
public class LoginController {

    @Resource
    private UserLoginService userLoginService;

    /**
     * 检查用户是否已登录。
     *
     * @return 返回用户会话信息，未登录则返回 401 错误。
     */
    @GetMapping(value = "/is-login")
    @Operation(summary = "是否登录")
    public SessionUser isLogin() {
        SessionUser user = SessionUtils.getUser();
        if (user != null) {
            UserDTO userDTO = userLoginService.getUserDTO(user.getId());
            SessionUser sessionUser = SessionUser.fromUser(userDTO, SessionUtils.getSessionId());
            SessionUtils.putUser(sessionUser);
            return sessionUser;
        }
        throw new GenericException(CrmHttpResultCode.UNAUTHORIZED);
    }

    /**
     * 获取 RSA 公钥。
     *
     * @return 返回 RSA 公钥。
     * @throws Exception 可能抛出的异常。
     */
    @GetMapping(value = "/get-key")
    @Operation(summary = "获取公钥")
    public String getKey() throws Exception {
        RsaKey rsaKey = RsaUtils.getRsaKey();
        return rsaKey.getPublicKey();
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求对象，包含用户名和密码。
     * @return 登录结果。
     * @throws GenericException 如果已登录且当前用户与请求用户名不同，抛出异常。
     */
    @PostMapping(value = "/login")
    @Operation(summary = "登录")
    public SessionUser login(@Validated @RequestBody LoginRequest request) {
        SessionUser sessionUser = SessionUtils.getUser();
        if (sessionUser != null) {
            // 如果当前用户已登录且用户名与请求用户名不匹配，抛出异常
            if (!StringUtils.equals(sessionUser.getId(), request.getUsername())) {
                throw new GenericException(Translator.get("please_logout_current_user"));
            }
        }
        // 设置认证方式为 LOCAL
        SecurityUtils.getSubject().getSession().setAttribute("authenticate", UserSource.LOCAL.name());
        return userLoginService.login(request);
    }

    /**
     * 退出登录。
     *
     * @return 返回退出成功信息。
     */
    @GetMapping(value = "/signout")
    @Operation(summary = "退出登录")
    public String logout() {
        if (SessionUtils.getUser() == null) {
            return "logout success";
        }
        // 退出当前会话
        SecurityUtils.getSubject().logout();
        return "logout success";
    }
}
