package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.common.constants.UserSource;
import io.cordys.common.exception.GenericException;
import io.cordys.common.request.LoginRequest;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import io.cordys.security.UserDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Transactional(rollbackFor = Exception.class)
public class UserLoginService {
    @Resource
    private BaseMapper<User> sysUserMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private RoleService roleService;

    public UserDTO getUserDTO(String userId) {
        UserDTO userDTO = extUserMapper.selectById(userId);
        if (userDTO == null) {
            return null;
        }
        if (BooleanUtils.isFalse(userDTO.getEnable())) {
            throw new DisabledAccountException();
        }
        // 设置权限
        userDTO.setPermissionIds(roleService.getPermissionIdsByUserId(userId));
        // 设置组织ID
        userDTO.setOrganizationIds(getOrgIdsByUserId(userId));
        return userDTO;
    }

    private Set<String> getOrgIdsByUserId(String userId) {
        OrganizationUser example = new OrganizationUser();
        example.setUserId(userId);
        example.setEnable(true);
        return organizationUserMapper.select(example).stream()
                .map(OrganizationUser::getOrganizationId).collect(Collectors.toSet());
    }

    public UserDTO getUserDTOByEmail(String email, String... source) {
        User example = new User();
        example.setEmail(email);
        List<User> users = sysUserMapper.select(example);
        if (users == null || users.isEmpty()) {
            return null;
        }

        return getUserDTO(users.getFirst().getId());
    }


    @OperationLog(
            module = LogModule.SYSTEM,
            type = LogType.LOGIN,
            operator = "",
            resourceId = "",
            success = "登录成功",
            loginAddress = "{{#request.loginAddress}}",
            platform = "{{#request.platform}}"
    )
    public SessionUser login(LoginRequest request) {
        String login = (String) SecurityUtils.getSubject().getSession().getAttribute("authenticate");
        String username = StringUtils.trim(request.getUsername());
        String password = StringUtils.EMPTY;
        if (!StringUtils.equals(login, UserSource.LDAP.name())) {
            password = StringUtils.trim(request.getPassword());
        }
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
            if (subject.isAuthenticated()) {
                SessionUser sessionUser = SessionUtils.getUser();
                // 放入session中
                SessionUtils.putUser(sessionUser);
                return sessionUser;
            } else {
                throw new GenericException(Translator.get("login_fail"));
            }
        } catch (ExcessiveAttemptsException e) {
            throw new ExcessiveAttemptsException(Translator.get("excessive_attempts"));
        } catch (LockedAccountException e) {
            throw new LockedAccountException(Translator.get("user_locked"));
        } catch (DisabledAccountException e) {
            throw new DisabledAccountException(Translator.get("user_has_been_disabled"));
        } catch (ExpiredCredentialsException e) {
            throw new ExpiredCredentialsException(Translator.get("user_expires"));
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e.getMessage());
        } catch (UnauthorizedException e) {
            throw new UnauthorizedException(Translator.get("not_authorized") + e.getMessage());
        }
    }

    public boolean checkUserPassword(String userId, String password) {
        if (StringUtils.isBlank(userId)) {
            throw new GenericException(Translator.get("user_name_is_null"));
        }
        if (StringUtils.isBlank(password)) {
            throw new GenericException(Translator.get("password_is_null"));
        }
        User example = new User();
        example.setId(userId);
        example.setPassword(CodingUtils.md5(password));
        return sysUserMapper.exist(example);
    }
}
