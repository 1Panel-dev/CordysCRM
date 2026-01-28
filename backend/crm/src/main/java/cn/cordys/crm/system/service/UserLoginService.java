package cn.cordys.crm.system.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ThirdDetailType;
import cn.cordys.common.dto.RoleDataScopeDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.request.LoginRequest;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.ServletUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.LoginType;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.ThirdAuthConfigDTO;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import cn.cordys.security.UserDTO;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户登录服务
 *
 * <p>提供用户认证、登录、密码验证等相关功能
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserLoginService {
  @Resource private BaseMapper<User> userMapper;

  @Resource private BaseMapper<OrganizationUser> organizationUserMapper;

  @Resource private ExtOrganizationMapper extOrganizationMapper;

  @Resource private ExtUserMapper extUserMapper;

  @Resource private RoleService roleService;

  @Resource private BaseMapper<LoginLog> loginLogMapper;

  @Resource private BaseMapper<Department> departmentMapper;

  @Resource private PermissionCache permissionCache;

  @Resource private ExtOrganizationConfigMapper extOrganizationConfigMapper;

  @Resource private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

  /**
   * 用户登录
   *
   * @param request 登录请求
   * @return 会话用户信息
   * @throws AuthenticationException 登录失败时抛出相应异常
   */
  public SessionUser login(LoginRequest request) {
    String username = StringUtils.trim(request.getUsername());
    String password = StringUtils.trim(request.getPassword());

    Subject subject = SecurityUtils.getSubject();
    UsernamePasswordToken token = new UsernamePasswordToken(username, password);

    try {
      subject.login(token);

      if (!subject.isAuthenticated()) {
        throw new GenericException(Translator.get("login_fail"));
      }

      // 登录成功，记录会话用户并添加登录日志
      SessionUser sessionUser = SessionUtils.getUser();
      SessionUtils.putUser(sessionUser);
      recordLoginLog(request);

      return sessionUser;
    } catch (ExcessiveAttemptsException e) {
      throw new ExcessiveAttemptsException(Translator.get("password_is_incorrect"));
    } catch (LockedAccountException e) {
      throw new LockedAccountException(Translator.get("password_is_incorrect"));
    } catch (DisabledAccountException e) {
      throw new DisabledAccountException(Translator.get("password_is_incorrect"));
    } catch (ExpiredCredentialsException e) {
      throw new ExpiredCredentialsException(Translator.get("password_is_incorrect"));
    } catch (AuthenticationException e) {
      throw new AuthenticationException(e.getMessage());
    } catch (UnauthorizedException e) {
      throw new UnauthorizedException(Translator.get("password_is_incorrect") + e.getMessage());
    }
  }

  /**
   * 认证用户并获取用户详细信息
   *
   * @param userKey 用户标识（用户名/手机号/邮箱）
   * @return 用户详细信息
   * @throws AuthenticationException 如果用户不存在或被禁用
   */
  public UserDTO authenticateUser(String userKey) {
    // 获取用户信息
    UserDTO userDTO =
        Optional.ofNullable(extUserMapper.selectByPhoneOrEmail(userKey))
            .orElseThrow(
                () -> new AuthenticationException(Translator.get("password_is_incorrect")));

    // 非管理员用户需要检查是否被禁用
    if (!isAdminUser(userDTO.getId())) {
      checkUserStatus(userDTO);
    }

    // 获取用户所属组织列表
    Set<String> orgIds = getUserOrganizations(userDTO.getId());

    // 确定当前使用的组织ID
    String organizationId = determineOrganizationId(userDTO, orgIds);

    // 设置用户权限和角色信息
    setupUserPermissions(userDTO, organizationId, orgIds);

    // 默认密码检查
    checkDefaultPwd(userDTO);

    return userDTO;
  }

  /**
   * 检查默认密码
   *
   * @param userDTO
   */
  private void checkDefaultPwd(UserDTO userDTO) {
    String defaultPwd = "";
    if (Strings.CI.equals(userDTO.getId(), InternalUser.ADMIN.getValue())) {
      defaultPwd = CodingUtils.md5("CordysCRM");
    } else {
      if (StringUtils.isNotBlank(userDTO.getPhone())) {
        defaultPwd = CodingUtils.md5(userDTO.getPhone().substring(userDTO.getPhone().length() - 6));
      }
    }

    if (Strings.CI.equals(defaultPwd, userDTO.getPassword())) {
      userDTO.setDefaultPwd(true);
    }
  }

  /**
   * 检查用户密码是否正确
   *
   * @param userId 用户ID
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
    return userMapper.exist(example);
  }

  /**
   * 检查移动端认证配置
   *
   * @param organizationId 组织ID
   * @throws AuthenticationException 如果未配置移动端认证或配置无效
   */
  public void checkMobileAuthConfig(String organizationId) {
    // 如果不是移动端请求，直接返回
    if (!isMobileRequest()) {
      return;
    }

    // 检查组织认证配置
    OrganizationConfig orgConfig = getOrganizationAuthConfig(organizationId);
    if (orgConfig == null) {
      throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
    }

    // 检查企业微信认证配置
    List<OrganizationConfigDetail> enabledConfigs = getEnabledWeComOauthConfigs(orgConfig.getId());
    if (CollectionUtils.isEmpty(enabledConfigs)) {
      throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
    }

    // 验证配置内容
    validateAuthConfig(enabledConfigs.getFirst());
  }

  /** 检查用户状态是否正常 */
  private void checkUserStatus(UserDTO userDTO) {
    LambdaQueryWrapper<OrganizationUser> queryWrapper =
        new LambdaQueryWrapper<OrganizationUser>()
            .eq(OrganizationUser::getUserId, userDTO.getId())
            .eq(OrganizationUser::getEnable, true);

    if (StringUtils.isNotBlank(userDTO.getLastOrganizationId())) {
      queryWrapper.eq(OrganizationUser::getOrganizationId, userDTO.getLastOrganizationId());
    }

    List<OrganizationUser> orgUsers = organizationUserMapper.selectListByLambda(queryWrapper);
    if (CollectionUtils.isEmpty(orgUsers)) {
      throw new DisabledAccountException(Translator.get("password_is_incorrect"));
    }

    // 设置用户部门信息
    setUserDepartmentInfo(userDTO, orgUsers.getFirst());
  }

  /** 设置用户的部门信息 */
  private void setUserDepartmentInfo(UserDTO userDTO, OrganizationUser orgUser) {
    userDTO.setDepartmentId(orgUser.getDepartmentId());

    Optional.ofNullable(departmentMapper.selectByPrimaryKey(orgUser.getDepartmentId()))
        .ifPresent(department -> userDTO.setDepartmentName(department.getName()));
  }

  /** 确定用户当前使用的组织ID */
  private String determineOrganizationId(UserDTO userDTO, Set<String> orgIds) {
    String orgId = OrganizationContext.getOrganizationId();

    if (StringUtils.isBlank(orgId) && CollectionUtils.isNotEmpty(orgIds)) {
      // 上下文中无组织ID时，优先使用用户最后访问的组织，否则取第一个可用组织
      return orgIds.contains(userDTO.getLastOrganizationId())
          ? userDTO.getLastOrganizationId()
          : orgIds.iterator().next();
    }

    return orgId;
  }

  /** 设置用户的权限和角色信息 */
  private void setupUserPermissions(UserDTO userDTO, String organizationId, Set<String> orgIds) {
    // 设置用户角色
    List<RoleDataScopeDTO> roleOptions =
        roleService.getRoleOptions(userDTO.getId(), organizationId);
    userDTO.setRoles(roleOptions);

    // 更新最后登录的组织ID
    userDTO.setLastOrganizationId(organizationId);

    // 设置用户权限
    userDTO.setPermissionIds(permissionCache.getPermissionIds(userDTO.getId(), organizationId));

    // 设置用户所属的所有组织
    userDTO.setOrganizationIds(orgIds);
  }

  /** 获取用户所属的所有组织ID */
  private Set<String> getUserOrganizations(String userId) {
    // 管理员可以访问所有组织
    if (isAdminUser(userId)) {
      return extOrganizationMapper.selectAllOrganizationIds();
    }

    // 普通用户只能访问已授权且启用的组织
    return organizationUserMapper.select(createOrgUserExample(userId)).stream()
        .map(OrganizationUser::getOrganizationId)
        .collect(Collectors.toSet());
  }

  /** 创建组织用户查询条件 */
  private OrganizationUser createOrgUserExample(String userId) {
    OrganizationUser example = new OrganizationUser();
    example.setUserId(userId);
    example.setEnable(true);
    return example;
  }

  /** 记录登录日志 */
  private void recordLoginLog(LoginRequest request) {
    LoginLog log = new LoginLog();
    log.setId(IDGenerator.nextStr());
    log.setLoginAddress(request.getLoginAddress());
    log.setOperator(SessionUtils.getUserId());
    log.setCreateTime(System.currentTimeMillis());
    log.setPlatform(determinePlatform());

    loginLogMapper.insert(log);
  }

  /** 获取组织认证配置 */
  private OrganizationConfig getOrganizationAuthConfig(String organizationId) {
    return extOrganizationConfigMapper.getOrganizationConfig(
        organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
  }

  /** 获取启用的企业微信OAuth配置 */
  private List<OrganizationConfigDetail> getEnabledWeComOauthConfigs(String configId) {
    return extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(
        configId,
        List.of(
            ThirdDetailType.WECOM_SYNC.name(),
            ThirdDetailType.DINGTALK_SYNC.name(),
            ThirdDetailType.LARK_SYNC.name()));
  }

  /** 验证认证配置是否有效 */
  private void validateAuthConfig(OrganizationConfigDetail configDetail) {
    String content = new String(configDetail.getContent(), StandardCharsets.UTF_8);
    ThirdAuthConfigDTO authConfig = JSON.parseObject(content, ThirdAuthConfigDTO.class);

    if (authConfig == null) {
      throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
    }
  }

  /** 根据User-Agent确定登录平台类型 */
  private String determinePlatform() {
    return isMobileRequest() ? LoginType.MOBILE.getName() : LoginType.WEB.getName();
  }

  /** 判断是否为移动端请求 */
  private boolean isMobileRequest() {
    String userAgent = ServletUtils.getUserAgent();
    return StringUtils.isNotBlank(userAgent) && isMobileUserAgent(userAgent);
  }

  /** 判断是否为管理员用户 */
  private boolean isAdminUser(String userId) {
    return Strings.CS.equals(userId, InternalUser.ADMIN.getValue());
  }

  /** 判断是否为移动端User-Agent */
  private boolean isMobileUserAgent(String userAgent) {
    return userAgent.contains("miniprogram")
        || userAgent.contains("MicroMessenger")
        || userAgent.contains("Android")
        || userAgent.contains("iOS")
        || userAgent.contains("Mobile")
        || userAgent.contains("MQQBrowser")
        || userAgent.contains("Mobile Safari")
        || userAgent.contains("iPhone")
        || userAgent.contains("iPad")
        || userAgent.contains("ipod");
  }
}
