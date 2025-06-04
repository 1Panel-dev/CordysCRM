package io.cordys.crm.system.service;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.UserSource;
import io.cordys.common.dto.RoleDataScopeDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.request.LoginRequest;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.ServletUtils;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.constants.LoginType;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.*;
import io.cordys.crm.system.dto.ThirdAuthConfigDTO;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.crm.system.mapper.ExtOrganizationMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import io.cordys.security.UserDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户登录服务
 * <p>
 * 提供用户认证、登录、密码验证等相关功能
 * </p>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserLoginService {
    @Resource
    private BaseMapper<User> sysUserMapper;

    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @Resource
    private ExtUserMapper extUserMapper;

    @Resource
    private RoleService roleService;

    @Resource
    private BaseMapper<LoginLog> loginLogMapper;

    @Resource
    private BaseMapper<OrganizationUser> organizationUserBaseMapper;

    @Resource
    private PermissionCache permissionCache;

    @Resource
    private BaseMapper<Department> departmentBaseMapper;

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    /**
     * 认证用户并获取用户详细信息
     *
     * @param userKey 用户标识（用户名/手机号/邮箱）
     * @return 用户详细信息
     * @throws AuthenticationException 如果用户不存在或被禁用
     */
    public UserDTO authenticateUser(String userKey) {
        // 获取用户信息
        UserDTO userDTO = Optional.ofNullable(extUserMapper.selectByPhoneOrEmail(userKey))
                .orElseThrow(() -> new AuthenticationException(Translator.get("user_not_exist")));

        // 非管理员用户需要检查是否被禁用
        if (!StringUtils.equals(userDTO.getId(), InternalUser.ADMIN.getValue())) {
            checkUserStatus(userDTO);
        }

        // 获取用户所属组织列表
        Set<String> orgIds = getOrgIdsByUserId(userDTO.getId());

        // 确定当前使用的组织ID
        String organizationId = determineOrganizationId(userDTO, orgIds);

        // 设置用户权限和角色信息
        setupUserPermissions(userDTO, organizationId, orgIds);

        return userDTO;
    }

    /**
     * 检查用户状态是否正常
     *
     * @param userDTO 用户信息
     * @throws DisabledAccountException 如果用户被禁用
     */
    private void checkUserStatus(UserDTO userDTO) {
        LambdaQueryWrapper<OrganizationUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrganizationUser::getUserId, userDTO.getId());
        queryWrapper.eq(OrganizationUser::getEnable, true);

        if (StringUtils.isNotBlank(userDTO.getLastOrganizationId())) {
            queryWrapper.eq(OrganizationUser::getOrganizationId, userDTO.getLastOrganizationId());
        }

        List<OrganizationUser> organizationUsers = organizationUserBaseMapper.selectListByLambda(queryWrapper);

        if (CollectionUtils.isEmpty(organizationUsers)) {
            throw new DisabledAccountException(Translator.get("user_has_been_disabled"));
        }

        // 设置部门信息
        OrganizationUser orgUser = organizationUsers.getFirst();
        userDTO.setDepartmentId(orgUser.getDepartmentId());

        Optional.ofNullable(departmentBaseMapper.selectByPrimaryKey(orgUser.getDepartmentId()))
                .ifPresent(department -> userDTO.setDepartmentName(department.getName()));
    }

    /**
     * 确定用户当前使用的组织ID
     *
     * @param userDTO 用户信息
     * @param orgIds  用户所属的所有组织ID
     * @return 确定的组织ID
     */
    private String determineOrganizationId(UserDTO userDTO, Set<String> orgIds) {
        String organizationId = OrganizationContext.getOrganizationId();

        if (StringUtils.isBlank(organizationId) && CollectionUtils.isNotEmpty(orgIds)) {
            // 上下文中无组织ID时，选取一个可用的组织ID
            organizationId = orgIds.contains(userDTO.getLastOrganizationId())
                    ? userDTO.getLastOrganizationId()
                    : orgIds.iterator().next();
        }

        return organizationId;
    }

    /**
     * 设置用户的权限和角色信息
     *
     * @param userDTO        用户信息
     * @param organizationId 组织ID
     * @param orgIds         用户所属的所有组织ID
     */
    private void setupUserPermissions(UserDTO userDTO, String organizationId, Set<String> orgIds) {
        // 设置用户角色
        List<RoleDataScopeDTO> roleOptions = roleService.getRoleOptions(userDTO.getId(), organizationId);
        userDTO.setRoles(roleOptions);

        // 更新最后登录的组织ID
        userDTO.setLastOrganizationId(organizationId);

        // 设置用户权限
        userDTO.setPermissionIds(permissionCache.getPermissionIds(userDTO.getId(), organizationId));

        // 设置用户所属的所有组织
        userDTO.setOrganizationIds(orgIds);
    }

    /**
     * 获取用户所属的所有组织ID
     *
     * @param userId 用户ID
     * @return 组织ID集合
     */
    private Set<String> getOrgIdsByUserId(String userId) {
        // 管理员可以访问所有组织
        if (StringUtils.equals(userId, InternalUser.ADMIN.getValue())) {
            return extOrganizationMapper.selectAllOrganizationIds();
        }

        // 普通用户只能访问已授权且启用的组织
        OrganizationUser example = new OrganizationUser();
        example.setUserId(userId);
        example.setEnable(true);

        return organizationUserMapper.select(example).stream()
                .map(OrganizationUser::getOrganizationId)
                .collect(Collectors.toSet());
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 会话用户信息
     * @throws GenericException 登录失败时抛出相应异常
     */
    public SessionUser login(LoginRequest request) {
        String username = StringUtils.trim(request.getUsername());
        String password = StringUtils.trim(request.getPassword());

        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        Subject subject = SecurityUtils.getSubject();

        try {
            subject.login(token);

            if (subject.isAuthenticated()) {
                SessionUser sessionUser = SessionUtils.getUser();
                SessionUtils.putUser(sessionUser);
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

    /**
     * 插入登录日志
     *
     * @param request 登录请求
     */
    private void insertLoginLog(LoginRequest request) {
        LoginLog loginLog = new LoginLog();
        loginLog.setId(IDGenerator.nextStr());
        loginLog.setLoginAddress(request.getLoginAddress());
        loginLog.setOperator(SessionUtils.getUserId());
        loginLog.setCreateTime(System.currentTimeMillis());

        // 设置登录平台
        setPlatform(loginLog);

        loginLogMapper.insert(loginLog);
    }

    /**
     * 根据User-Agent设置登录平台信息
     *
     * @param loginLog 登录日志对象
     */
    private void setPlatform(LoginLog loginLog) {
        String userAgent = ServletUtils.getUserAgent();

        if (StringUtils.isBlank(userAgent)) {
            loginLog.setPlatform(LoginType.WEB.getName());
            return;
        }

        // 检测移动端特征
        boolean isMobile = userAgent.contains("miniprogram") ||
                userAgent.contains("MicroMessenger") ||
                userAgent.contains("Android") ||
                userAgent.contains("iPhone") ||
                userAgent.contains("iPad") ||
                userAgent.contains("ipod");

        loginLog.setPlatform(isMobile ? LoginType.MOBILE.getName() : LoginType.WEB.getName());
    }

    /**
     * 检查用户密码是否正确
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 密码是否正确
     * @throws GenericException 如果用户ID或密码为空
     */
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

    /**
     * 检查移动端认证配置
     * <p>
     * 当请求来自移动端时，检查是否配置了相应的认证方式
     * </p>
     *
     * @throws AuthenticationException 如果未配置移动端认证或配置无效
     */
    public void checkMobileAuthConfig(String organizationId) {
        String userAgent = ServletUtils.getUserAgent();

        // 如果User-Agent为空或不是移动端，不进行检查
        if (StringUtils.isBlank(userAgent) || !isMobileUserAgent(userAgent)) {
            return;
        }

        // 检查组织配置是否存在
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.AUTH.name());

        if (organizationConfig == null) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }

        // 检查是否启用了企业微信认证
        List<OrganizationConfigDetail> enabledConfigs = extOrganizationConfigDetailMapper
                .getEnableOrganizationConfigDetails(organizationConfig.getId(), UserSource.WE_COM_OAUTH2.toString());

        if (CollectionUtils.isEmpty(enabledConfigs)) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }

        // 验证配置内容
        OrganizationConfigDetail configDetail = enabledConfigs.getFirst();
        String content = new String(configDetail.getContent(), StandardCharsets.UTF_8);

        ThirdAuthConfigDTO weComConfig = JSON.parseObject(content, ThirdAuthConfigDTO.class);
        if (weComConfig == null) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }
    }

    /**
     * 判断是否为移动端User-Agent
     *
     * @param userAgent User-Agent字符串
     * @return 是否为移动端
     */
    private boolean isMobileUserAgent(String userAgent) {
        return userAgent.contains("miniprogram") ||
                userAgent.contains("MicroMessenger") ||
                userAgent.contains("Android") ||
                userAgent.contains("iOS") ||
                userAgent.contains("Mobile") ||
                userAgent.contains("MQQBrowser") ||
                userAgent.contains("Mobile Safari");
    }
}