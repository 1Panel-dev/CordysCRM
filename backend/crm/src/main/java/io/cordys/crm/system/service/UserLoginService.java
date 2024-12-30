package io.cordys.crm.system.service;

import io.cordys.common.constants.UserSource;
import io.cordys.common.exception.GenericException;
import io.cordys.common.request.LoginRequest;
import io.cordys.common.response.handler.ResultHolder;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.SysUser;
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


@Service
@Transactional(rollbackFor = Exception.class)
public class UserLoginService {
    @Resource
    private BaseMapper<SysUser> sysUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;

    public UserDTO getUserDTO(String userId) {
        UserDTO userDTO = extUserMapper.selectById(userId);
        if (userDTO == null) {
            return null;
        }
        if (BooleanUtils.isFalse(userDTO.getEnable())) {
            throw new DisabledAccountException();
        }
        return userDTO;
    }

    public UserDTO getUserDTOByEmail(String email, String... source) {
        SysUser example = new SysUser();
        example.setEmail(email);
        List<SysUser> users = sysUserMapper.select(example);
        if (users == null || users.isEmpty()) {
            return null;
        }

        return getUserDTO(users.getFirst().getId());
    }


    public ResultHolder login(LoginRequest request) {
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
                return ResultHolder.success(sessionUser);
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
        SysUser example = new SysUser();
        example.setId(userId);
        example.setPassword(CodingUtils.md5(password));
        example.setEnable(1);
        return sysUserMapper.exist(example);
    }
}
