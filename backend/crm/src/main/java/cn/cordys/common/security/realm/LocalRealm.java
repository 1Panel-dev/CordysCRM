package cn.cordys.common.security.realm;


import cn.cordys.common.constants.UserSource;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.service.UserLoginService;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import cn.cordys.security.UserDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.context.annotation.Lazy;


/**
 * 自定义Realm 注入service 可能会导致在 service的aop 失效，例如@Transactional,
 * 解决方法：
 * <p>
 * 1. 这里改成注入mapper，这样mapper 中的事务失效<br/>
 * 2. 这里仍然注入service，在配置ShiroConfig 的时候不去set realm, 等到spring 初始化完成之后
 * set realm
 * </p>
 */
@Slf4j
public class LocalRealm extends AuthorizingRealm {
    @Resource
    @Lazy
    private UserLoginService userLoginService;

    @Override
    public String getName() {
        return "LOCAL";
    }

    /**
     * 角色认证
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return null;
    }

    /**
     * 登录认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        Session session = SecurityUtils.getSubject().getSession();
        String login = (String) session.getAttribute("authenticate");

        String userId = token.getUsername();
        String password = String.valueOf(token.getPassword());

        if (Strings.CS.equals(login, UserSource.LOCAL.name())) {
            return loginLocalMode(userId, password);
        }

        UserDTO user = getUserWithOutAuthenticate(userId);
        userId = user.getId();
        SessionUser sessionUser = SessionUser.fromUser(user, (String) session.getId());
        session.setAttribute(SessionConstants.ATTR_USER, sessionUser);
        return new SimpleAuthenticationInfo(userId, password, getName());

    }

    @Override
    public boolean isPermitted(PrincipalCollection principals, String permission) {
        return PermissionUtils.hasPermission(permission);
    }

    private UserDTO getUserWithOutAuthenticate(String userId) {
        UserDTO user = userLoginService.authenticateUser(userId);
        if (user == null) {
            log.warn("The user does not exist: " + userId);
            throw new UnknownAccountException(Translator.get("password_is_incorrect"));
        }
        return user;
    }

    private AuthenticationInfo loginLocalMode(String userId, String password) {
        UserDTO user = userLoginService.authenticateUser(userId);
        if (user == null) {
            log.warn("The user does not exist: " + userId);
            throw new UnknownAccountException(Translator.get("password_is_incorrect"));
        }
        // 密码验证
        if (!userLoginService.checkUserPassword(user.getId(), password)) {
            throw new IncorrectCredentialsException(Translator.get("password_is_incorrect"));
        }
        SessionUser sessionUser = SessionUser.fromUser(user, SessionUtils.getSessionId());
        SessionUtils.putUser(sessionUser);
        return new SimpleAuthenticationInfo(userId, password, getName());
    }

}
