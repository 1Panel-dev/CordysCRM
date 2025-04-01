package io.cordys.crm.system.service;

import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.request.LoginRequest;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.LoginLog;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import io.cordys.security.UserDTO;
import jakarta.annotation.Resource;
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
    @Resource
    private BaseMapper<LoginLog> loginLogMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserBaseMapper;

    public UserDTO authenticateUser(String userId) {
        UserDTO userDTO = extUserMapper.selectByPhoneOrEmail(userId);
        if (userDTO == null) {
            throw new AuthenticationException(Translator.get("user_not_exist"));
        }
        // 检查用户是否被禁用
        if (StringUtils.isNotBlank(userDTO.getLastOrganizationId())) {
            var userLambdaQueryWrapper = new LambdaQueryWrapper<OrganizationUser>()
                    .eq(OrganizationUser::getUserId, userDTO.getId())
                    .eq(OrganizationUser::getOrganizationId, userDTO.getLastOrganizationId())
                    .eq(OrganizationUser::getEnable, true);

            if (organizationUserBaseMapper.selectListByLambda(userLambdaQueryWrapper).isEmpty()) {
                throw new DisabledAccountException(Translator.get("user_has_been_disabled"));
            }
        }

        List<String> roleIds = roleService.getRoleIdsByUserId(userDTO.getId());
        List<OptionDTO> roleOptions = roleService.getRoleOptions(roleIds);
        // 设置角色信息，供前端展示
        userDTO.setRoles(roleOptions);
        // 设置权限
        userDTO.setPermissionIds(roleService.getPermissionIds(roleIds));
        // 设置组织ID
        userDTO.setOrganizationIds(getOrgIdsByUserId(userDTO.getId()));
        return userDTO;
    }

    private Set<String> getOrgIdsByUserId(String userId) {
        OrganizationUser example = new OrganizationUser();
        example.setUserId(userId);
        example.setEnable(true);
        return organizationUserMapper.select(example).stream()
                .map(OrganizationUser::getOrganizationId).collect(Collectors.toSet());
    }

    public SessionUser login(LoginRequest request) {
        String username = StringUtils.trim(request.getUsername());
        String password = StringUtils.trim(request.getPassword());
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
            if (subject.isAuthenticated()) {
                SessionUser sessionUser = SessionUtils.getUser();
                // 放入session中
                SessionUtils.putUser(sessionUser);
                // 记录登入日志
                insertLoginLog(request);
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

    private void insertLoginLog(LoginRequest request) {
        LoginLog loginLog = new LoginLog();
        loginLog.setId(IDGenerator.nextStr());
        loginLog.setLoginAddress(request.getLoginAddress());
        loginLog.setPlatform(request.getPlatform());
        loginLog.setOperator(SessionUtils.getUserId());
        loginLog.setCreateTime(System.currentTimeMillis());
        loginLogMapper.insert(loginLog);
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
