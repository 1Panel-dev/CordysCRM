package cn.cordys.crm.integration.sso.service;

import cn.cordys.common.constants.DepartmentConstants;
import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.constants.UserSource;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.request.LoginRequest;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.common.service.OAuthUserService;
import cn.cordys.crm.integration.dingtalk.response.DingTalkUserResponse;
import cn.cordys.crm.integration.wecom.response.WeComUserResponse;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.domain.UserExtend;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.crm.system.service.UserLoginService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUser;
import cn.cordys.security.UserDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.DisabledAccountException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class SSOService {

    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    private static final String ERROR_CODE_NOT_EXIST = "code_not_exist";
    private static final String ERROR_AUTH_SETTING_NOT_EXISTS = "auth.setting.no.exists";
    private static final String ERROR_AUTH_GET_USER_ERROR = "auth.get.user.error";
    private static final String ERROR_USER_NOT_EXIST = "user_not_exist";
    private static final String ERROR_AUTH_LOGIN_DISABLE = "auth.login.un_enable";
    private static final String ERROR_THIRD_CONFIG_NOT_EXIST = "third.config.not.exist";
    private static final String ERROR_THIRD_CONFIG_DISABLE = "third.config.un.enable";
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;
    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;
    @Resource
    private BaseMapper<User> userBaseMapper;
    @Resource
    private BaseMapper<UserExtend> userExtendBaseMapper;
    @Resource
    private UserLoginService userLoginService;
    @Resource
    private OAuthUserService oauthUserService;
    @Resource
    private TokenService tokenService;

    public SessionUser exchangeGitOauth2(String code) {
        validateCode(code);

        OrganizationConfigDetail configDetail = getAuthConfigDetail(UserSource.GITHUB_OAUTH2.toString());
        String content = new String(configDetail.getContent(), StandardCharsets.UTF_8);
        Map<String, String> config = JSON.parseObject(content, new TypeReference<HashMap<String, String>>() {
        });

        Map<String, Object> resultObj = getGitHubUserInfo(code, config);

        // 解析用户信息映射
        Map<String, String> mapping = JSON.parseObject(config.get("mapping"), new TypeReference<HashMap<String, String>>() {
        });
        String userId = (String) resultObj.get(mapping.get("id"));
        String name = (String) resultObj.get(mapping.get("name"));
        String email = (String) resultObj.get(mapping.get("email"));
        String avatar = (String) resultObj.get(mapping.get("avatar_url"));

        // 查找并更新用户信息
        UserDTO enableUser = getUserAndValidate(userId);
        updateUserInfo(enableUser.getId(), name, email, avatar);

        // 创建登录请求
        LoginRequest loginRequest = createLoginRequest(
                name,
                enableUser.getPassword(),
                DepartmentConstants.WECOM.name()
        );
        return userLoginService.login(loginRequest);
    }

    public SessionUser exchangeWeComOauth2(String code) {
        validateCode(code);

        OrganizationConfigDetail configDetail = getAuthConfigDetail(ThirdConstants.ThirdDetailType.WECOM_SYNC.toString());
        String content = new String(configDetail.getContent(), StandardCharsets.UTF_8);
        ThirdConfigurationDTO weComConfig = JSON.parseObject(content, ThirdConfigurationDTO.class);

        if (weComConfig == null) {
            throw new AuthenticationException(Translator.get(ERROR_AUTH_SETTING_NOT_EXISTS));
        }

        return getWeComSessionUser(code, weComConfig, UserSource.WECOM_OAUTH2.toString());
    }

    public SessionUser exchangeWeComCode(String code) {
        validateCode(code);

        ThirdConfigurationDTO weComConfig = getThirdPartyConfig(DepartmentConstants.WECOM.name());
        validateQrCodeEnabled(weComConfig);

        return getWeComSessionUser(code, weComConfig, UserSource.QR_CODE.toString());
    }

    public SessionUser exchangeDingTalkCode(String code) {
        validateCode(code);

        ThirdConfigurationDTO dingTalkConfig = getThirdPartyConfig(DepartmentConstants.DINGTALK.name());
        validateQrCodeEnabled(dingTalkConfig);

        return getDingTalkSessionUser(code, dingTalkConfig, UserSource.QR_CODE.toString());
    }

    private SessionUser getWeComSessionUser(String code, ThirdConfigurationDTO weComConfig, String authenticateType) {
        // 获取assess_token
        String assessToken = tokenService.getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());
        if (StringUtils.isBlank(assessToken)) {
            throw new AuthenticationException(Translator.get(ERROR_AUTH_GET_USER_ERROR));
        }

        // 读取用户信息
        WeComUserResponse weComUser = oauthUserService.getWeComUser(assessToken, code);
        String email = StringUtils.isNotBlank(weComUser.getBizMail()) ? weComUser.getBizMail() : weComUser.getEmail();

        // 查找并验证用户
        UserDTO enableUser = getUserAndValidateEnable(weComUser.getUserId());

        // 只有OAuth2登录时才更新用户信息
        if (Strings.CI.equals(authenticateType, UserSource.WECOM_OAUTH2.toString())) {
            updateWeComUserInfo(enableUser, weComUser, email);
        }

        // 创建登录请求并登录
        LoginRequest loginRequest = createThirdPartyLoginRequest(
                enableUser,
                DepartmentConstants.WECOM.name(),
                authenticateType
        );

        SecurityUtils.getSubject().getSession().setAttribute("authenticate", authenticateType);
        return userLoginService.login(loginRequest);
    }

    private SessionUser getDingTalkSessionUser(String code, ThirdConfigurationDTO dingTalkConfig, String authenticateType) {
        // 获取用户assess_token
        String assessToken = tokenService.getDingTalkUserToken(dingTalkConfig.getAgentId(), dingTalkConfig.getAppSecret(), code);
        if (StringUtils.isBlank(assessToken)) {
            throw new GenericException(Translator.get(ERROR_AUTH_GET_USER_ERROR));
        }

        // 读取用户信息
        DingTalkUserResponse dingTalkUserResponse = oauthUserService.getDingTalkUser(assessToken);

        String dingTalkToken = tokenService.getDingTalkToken(dingTalkConfig.getAgentId(), dingTalkConfig.getAppSecret());
        //根据unionid查询用户信息
        String userIdByUnionId = oauthUserService.getUserIdByUnionId(dingTalkToken, dingTalkUserResponse.getUnionId());

        // 查找并验证用户
        UserDTO enableUser = getUserAndValidateEnable(userIdByUnionId);

        // 只有OAuth2登录时才更新用户信息
        if (Strings.CI.equals(authenticateType, UserSource.DINGTALK_OAUTH2.toString())) {
            updateDingTalkUserInfo(enableUser, dingTalkUserResponse);
        }

        // 创建登录请求并登录
        LoginRequest loginRequest = createThirdPartyLoginRequest(
                enableUser,
                DepartmentConstants.DINGTALK.name(),
                authenticateType
        );

        SecurityUtils.getSubject().getSession().setAttribute("authenticate", authenticateType);
        return userLoginService.login(loginRequest);
    }

    // 辅助方法
    private void validateCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new GenericException(Translator.get(ERROR_CODE_NOT_EXIST));
        }
    }

    private OrganizationConfigDetail getAuthConfigDetail(String source) {
        // 获取组织配置
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(
                DEFAULT_ORGANIZATION_ID,
                OrganizationConfigConstants.ConfigType.THIRD.name()
        );
        if (organizationConfig == null) {
            throw new GenericException(Translator.get(ERROR_AUTH_SETTING_NOT_EXISTS));
        }

        // 获取指定认证源的配置明细
        List<OrganizationConfigDetail> enableOrganizationConfigDetails =
                extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(
                        organizationConfig.getId(),
                        List.of(source)
                );
        if (CollectionUtils.isEmpty(enableOrganizationConfigDetails)) {
            throw new GenericException(Translator.get(ERROR_AUTH_SETTING_NOT_EXISTS));
        }

        return enableOrganizationConfigDetails.getFirst();
    }

    private Map<String, Object> getGitHubUserInfo(String code, Map<String, String> config) {
        try {
            String accessToken = tokenService.getGitHubOAuth2Token(code, config);
            String userInfoUrl = config.get("userInfoUrl");
            return oauthUserService.getGitHubUser(userInfoUrl, accessToken);
        } catch (Exception e) {
            throw new GenericException(Translator.get(ERROR_AUTH_GET_USER_ERROR));
        }
    }

    private UserDTO getUserAndValidate(String userId) {
        UserDTO user = extOrganizationUserMapper.getEnableUser(userId);
        if (user == null) {
            throw new GenericException(Translator.get(ERROR_USER_NOT_EXIST));
        }
        if (!user.getEnable()) {
            throw new GenericException(Translator.get(ERROR_AUTH_LOGIN_DISABLE));
        }
        return user;
    }

    private UserDTO getUserAndValidateEnable(String userId) {
        UserDTO user = extOrganizationUserMapper.getEnableUser(userId);
        if (user == null) {
            throw new AuthenticationException(Translator.get(ERROR_USER_NOT_EXIST));
        }
        if (!user.getEnable()) {
            throw new DisabledAccountException(Translator.get(ERROR_AUTH_LOGIN_DISABLE));
        }
        return user;
    }

    private void updateUserInfo(String userId, String name, String email, String avatar) {
        User user = new User();
        user.setId(userId);
        user.setName(name);
        user.setEmail(email);
        user.setUpdateTime(System.currentTimeMillis());
        userBaseMapper.updateById(user);
        if (StringUtils.isNotBlank(avatar)) {
            UserExtend userExtend = new UserExtend();
            userExtend.setId(userId);
            userExtend.setAvatar(avatar);
            userExtendBaseMapper.update(userExtend);
        }
    }

    private void updateWeComUserInfo(UserDTO user, WeComUserResponse weComUser, String email) {
        User userUpdate = new User();
        userUpdate.setId(user.getId());
        if (StringUtils.isBlank(user.getEmail())) {
            userUpdate.setEmail(email);
        }
        if (StringUtils.isNotBlank(weComUser.getMobile())) {
            userUpdate.setPhone(weComUser.getMobile());
        }
        userUpdate.setGender(weComUser.getGender() != null && weComUser.getGender() == 2);
        userUpdate.setUpdateTime(System.currentTimeMillis());
        userBaseMapper.updateById(userUpdate);

        if (StringUtils.isNotBlank(weComUser.getAvatar())) {
            UserExtend userExtend = new UserExtend();
            userExtend.setId(user.getId());
            userExtend.setAvatar(weComUser.getAvatar());
            userExtendBaseMapper.update(userExtend);
        }
    }

    private void updateDingTalkUserInfo(UserDTO user, DingTalkUserResponse dingTalkUserResponse) {
        User userUpdate = new User();
        userUpdate.setId(user.getId());
        if (StringUtils.isBlank(user.getEmail())) {
            userUpdate.setEmail(dingTalkUserResponse.getEmail());
        }
        if (StringUtils.isNotBlank(dingTalkUserResponse.getMobile())) {
            userUpdate.setPhone(dingTalkUserResponse.getMobile());
        }
        userUpdate.setUpdateTime(System.currentTimeMillis());
        userBaseMapper.updateById(userUpdate);

        if (StringUtils.isNotBlank(dingTalkUserResponse.getAvatarUrl())) {
            UserExtend userExtend = new UserExtend();
            userExtend.setId(user.getId());
            userExtend.setAvatar(dingTalkUserResponse.getAvatarUrl());
            userExtendBaseMapper.update(userExtend);
        }
    }

    private LoginRequest createLoginRequest(String username, String password, String platform) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setPlatform(platform);
        return request;
    }

    private LoginRequest createThirdPartyLoginRequest(UserDTO user, String platform, String authenticateType) {
        LoginRequest request = new LoginRequest();
        request.setUsername(user.getId());
        request.setPassword(generateDefaultPassword(user));
        request.setPlatform(platform);
        request.setAuthenticate(authenticateType);
        return request;
    }

    private String generateDefaultPassword(UserDTO user) {
        return Optional.ofNullable(user.getPhone())
                .filter(StringUtils::isNotBlank)
                .map(phone -> {
                    try {
                        if (phone.length() >= 6) {
                            return CodingUtils.md5(phone.substring(phone.length() - 6));
                        }
                        return CodingUtils.md5(phone);
                    } catch (Exception e) {
                        return CodingUtils.md5(phone);
                    }
                })
                .orElseGet(() -> CodingUtils.md5(user.getLastOrganizationId() + user.getId()));
    }

    private ThirdConfigurationDTO getThirdPartyConfig(String type) {
        IntegrationConfigService integrationConfigService = CommonBeanFactory.getBean(IntegrationConfigService.class);
        assert integrationConfigService != null;
        List<ThirdConfigurationDTO> synOrganizationConfigs = integrationConfigService.getThirdConfig(DEFAULT_ORGANIZATION_ID);

        if (CollectionUtils.isEmpty(synOrganizationConfigs)) {
            throw new GenericException(Translator.get(ERROR_THIRD_CONFIG_NOT_EXIST));
        }

        ThirdConfigurationDTO config = synOrganizationConfigs.stream()
                .filter(c -> Strings.CI.equals(c.getType(), type))
                .findFirst()
                .orElse(null);

        if (config == null) {
            throw new GenericException(Translator.get(ERROR_THIRD_CONFIG_NOT_EXIST));
        }

        return config;
    }

    private void validateQrCodeEnabled(ThirdConfigurationDTO config) {
        if (!config.getStartEnable()) {
            throw new GenericException(Translator.get(ERROR_THIRD_CONFIG_DISABLE));
        }
    }

    public SessionUser exchangeDingTalkOauth2(String code) {
        validateCode(code);

        OrganizationConfigDetail configDetail = getAuthConfigDetail(ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString());
        String content = new String(configDetail.getContent(), StandardCharsets.UTF_8);
        ThirdConfigurationDTO dingTalkConfig = JSON.parseObject(content, ThirdConfigurationDTO.class);

        if (dingTalkConfig == null) {
            throw new AuthenticationException(Translator.get(ERROR_AUTH_SETTING_NOT_EXISTS));
        }

        return getDingTalkSessionUser(code, dingTalkConfig, UserSource.DINGTALK_OAUTH2.toString());


    }

    public SessionUser exchangeLarkCode(String code) {
        validateCode(code);

        ThirdConfigurationDTO larkConfig = getThirdPartyConfig(DepartmentConstants.LARK.name());
        validateQrCodeEnabled(larkConfig);

        return getLarkSessionUser(code, larkConfig, UserSource.QR_CODE.toString(), false);
    }

    private SessionUser getLarkSessionUser(String code, ThirdConfigurationDTO larkConfig, String loginType, Boolean isMobile) {
        if (isMobile) {
            // 移动端需要变更域名
            larkConfig.setRedirectUrl(larkConfig.getRedirectUrl() + "/mobile");
        }
        // 获取用户assess_token
        String assessToken = tokenService.getLarkUserAccessToken(larkConfig.getAgentId(), larkConfig.getAppSecret(), larkConfig.getRedirectUrl(), code);
        if (StringUtils.isBlank(assessToken)) {
            throw new GenericException(Translator.get(ERROR_AUTH_GET_USER_ERROR));
        }

        // 读取用户信息
        Map<String, Object> larkUser = oauthUserService.getLarkUser(assessToken);

        //获取larkUser中data 对象
        Map<String, Object> data = (Map<String, Object>) larkUser.get("data");
        String userId = (String) data.get("open_id");
        // 查找并验证用户
        UserDTO enableUser = getUserAndValidateEnable(userId);

        // 创建登录请求并登录
        LoginRequest loginRequest = createThirdPartyLoginRequest(
                enableUser,
                DepartmentConstants.LARK.name(),
                loginType
        );

        SecurityUtils.getSubject().getSession().setAttribute("authenticate", loginType);
        return userLoginService.login(loginRequest);
    }

    public SessionUser exchangeLarkOauth2(String code, Boolean isMobile) {
        validateCode(code);

        ThirdConfigurationDTO larkConfig = getThirdPartyConfig(DepartmentConstants.LARK.name());
        validateQrCodeEnabled(larkConfig);

        return getLarkSessionUser(code, larkConfig, UserSource.LARK_OAUTH2.toString(), isMobile);
    }
}