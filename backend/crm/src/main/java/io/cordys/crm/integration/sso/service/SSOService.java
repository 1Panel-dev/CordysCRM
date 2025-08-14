package io.cordys.crm.integration.sso.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.constants.UserSource;
import io.cordys.common.exception.GenericException;
import io.cordys.common.request.LoginRequest;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.integration.auth.service.OAuthUserService;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.domain.UserExtend;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.crm.system.service.UserLoginService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.security.SessionUser;
import io.cordys.security.UserDTO;
import io.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import io.cordys.crm.system.service.IntegrationConfigService;
import io.cordys.crm.integration.dingtalk.response.DingTalkUserResponse;
import io.cordys.crm.integration.wecom.response.WeComUserResponse;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    private static final String DEFAULT_ORGANIZATION_ID = "100001";


    public SessionUser exchangeGitOauth2(String code) {
        if (StringUtils.isBlank(code)) {
            throw new GenericException(Translator.get("code_not_exist"));
        }
        //TODO: 这里待定。默认先取第一个组织
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(DEFAULT_ORGANIZATION_ID, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (organizationConfig == null) {
            throw new GenericException(Translator.get("auth.setting.no.exists"));
        }
        List<OrganizationConfigDetail> enableOrganizationConfigDetails = extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(organizationConfig.getId(), UserSource.GITHUB_OAUTH2.toString());
        if (CollectionUtils.isEmpty(enableOrganizationConfigDetails)) {
            throw new GenericException(Translator.get("auth.setting.no.exists"));
        }
        OrganizationConfigDetail organizationConfigDetail = enableOrganizationConfigDetails.getFirst();
        String content = new String(organizationConfigDetail.getContent(), StandardCharsets.UTF_8);
        Map<String, String> config = JSON.parseObject(content, new TypeReference<HashMap<String, String>>() {
        });

        Map<String, String> oauth2Config;
        Map<String, Object> resultObj;
        try {
            String accessToken = tokenService.getGitHubOAuth2Token(code, config);
            oauth2Config = JSON.parseObject(content, new TypeReference<HashMap<String, String>>() {
            });
            //https://docs.github.com/zh/rest/users/users?apiVersion=2022-11-28 详见github api
            String userInfoUrl = oauth2Config.get("userInfoUrl");
            resultObj = oauthUserService.getGitHubUser(userInfoUrl, accessToken);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }

        String attrMapping = oauth2Config.get("mapping");
        Map<String, String> mapping;
        try {
            mapping = JSON.parseObject(attrMapping, new TypeReference<HashMap<String, String>>() {
            });
            String userId = (String) resultObj.get(mapping.get("id"));
            String name = (String) resultObj.get(mapping.get("name"));
            String email = (String) resultObj.get(mapping.get("email"));
            String avatar = (String) resultObj.get(mapping.get("avatar_url"));
            //4.查找用户并更新
            UserDTO enableUser = extOrganizationUserMapper.getEnableUser(userId);
            User user = new User();
            user.setId(enableUser.getId());
            user.setName(name);
            user.setEmail(email);
            user.setUpdateTime(System.currentTimeMillis());
            userBaseMapper.updateById(user);
            UserExtend userExtend = new UserExtend();
            userExtend.setId(enableUser.getId());
            userExtend.setAvatar(avatar);
            userExtendBaseMapper.update(userExtend);
            if (!enableUser.getEnable()) {
                throw new GenericException(Translator.get("auth.login.un_enable"));
            }
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(name);
            loginRequest.setPassword(enableUser.getPassword());
            loginRequest.setPlatform(DepartmentConstants.WECOM.name());
            return userLoginService.login(loginRequest);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }

    }

    public SessionUser exchangeWeComOauth2(String code) {
        if (StringUtils.isBlank(code)) {
            throw new AuthenticationException(Translator.get("code_not_exist"));
        }
        //TODO: 这里待定。默认先取第一个组织
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(DEFAULT_ORGANIZATION_ID, OrganizationConfigConstants.ConfigType.AUTH.name());
        if (organizationConfig == null) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }
        List<OrganizationConfigDetail> enableOrganizationConfigDetails = extOrganizationConfigDetailMapper.getEnableOrganizationConfigDetails(organizationConfig.getId(), UserSource.WE_COM_OAUTH2.toString());
        if (CollectionUtils.isEmpty(enableOrganizationConfigDetails)) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }
        OrganizationConfigDetail organizationConfigDetail = enableOrganizationConfigDetails.getFirst();
        String content = new String(organizationConfigDetail.getContent(), StandardCharsets.UTF_8);
        ThirdConfigurationDTO weComConfig = JSON.parseObject(content, ThirdConfigurationDTO.class);
        if (weComConfig == null) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }
        return getWeComSessionUser(code, weComConfig, UserSource.WE_COM_OAUTH2.toString());


    }

    private SessionUser getWeComSessionUser(String code, ThirdConfigurationDTO weComConfig, String type) {
        //1. 获取assess_token
        String assessToken = tokenService.getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());

        if (StringUtils.isNotBlank(assessToken)) {
            // 2.读取成员
            WeComUserResponse weComUser = oauthUserService.getWeComUser(assessToken, code);

            String email = StringUtils.isNotBlank(weComUser.getBizMail()) ? weComUser.getBizMail() : weComUser.getEmail();
            //3.查找用户并更新
            UserDTO enableUser = extOrganizationUserMapper.getEnableUser(weComUser.getUserId());

            if (enableUser == null) {
                throw new AuthenticationException(Translator.get("user_not_exist"));
            }
            if (!enableUser.getEnable()) {
                throw new DisabledAccountException(Translator.get("auth.login.un_enable"));
            }
            //4.oauth2 登录用户才更新
            if (StringUtils.equalsIgnoreCase(type, UserSource.WE_COM_OAUTH2.toString())) {
                User user = new User();
                user.setId(enableUser.getId());
                if (StringUtils.isBlank(enableUser.getEmail())) {
                    user.setEmail(email);
                }
                if (StringUtils.isBlank(weComUser.getMobile())) {
                    user.setPhone(weComUser.getMobile());
                }
                // 更新性别
                user.setGender(weComUser.getGender() != null && weComUser.getGender() == 2);

                user.setUpdateTime(System.currentTimeMillis());
                userBaseMapper.updateById(user);

                // 更新头像
                UserExtend userExtend = new UserExtend();
                userExtend.setId(enableUser.getId());
                userExtend.setAvatar(weComUser.getAvatar());
                userExtendBaseMapper.update(userExtend);
            }

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(enableUser.getId());
            //密码没实际用途，用默认密码即可
            String password = Optional.ofNullable(enableUser.getPhone())
                    .filter(StringUtils::isNotBlank)
                    .map(phone -> {
                        try {
                            // 确保电话号码长度足够进行截取
                            if (phone.length() >= 6) {
                                return CodingUtils.md5(phone.substring(phone.length() - 6));
                            }
                            // 处理电话号码长度不足6位的情况
                            return CodingUtils.md5(phone);
                        } catch (Exception e) {
                            // 如果截取过程发生异常，回退到使用完整电话号码
                            return CodingUtils.md5(phone);
                        }
                    })
                    .orElseGet(() -> CodingUtils.md5(enableUser.getLastOrganizationId() + enableUser.getId()));

            loginRequest.setPassword(password);
            loginRequest.setPlatform(DepartmentConstants.WECOM.name());
            loginRequest.setAuthenticate(type);
            SecurityUtils.getSubject().getSession().setAttribute("authenticate", type);
            return userLoginService.login(loginRequest);
        } else {
            throw new AuthenticationException(Translator.get("auth.get.user.error"));
        }
    }

    private SessionUser getDingTalkSessionUser(String code, ThirdConfigurationDTO dingTalkConfig, String type) {
        //1. 获取用户assess_token
        String assessToken = tokenService.getDingTalkUserToken(dingTalkConfig.getCorpId(), dingTalkConfig.getAppSecret(), code);

        if (StringUtils.isNotBlank(assessToken)) {
            // 2.读取成员
            DingTalkUserResponse dingTalkUser = oauthUserService.getDingTalkUser(assessToken);

            //3.查找用户并更新
            UserDTO enableUser = extOrganizationUserMapper.getEnableUser(dingTalkUser.getUnionId());

            if (enableUser == null) {
                throw new AuthenticationException(Translator.get("user_not_exist"));
            }
            if (!enableUser.getEnable()) {
                throw new GenericException(Translator.get("auth.login.un_enable"));
            }
            //4.oauth2 登录用户才更新
            User user = new User();
            user.setId(enableUser.getId());
            if (StringUtils.isBlank(enableUser.getEmail())) {
                user.setEmail(dingTalkUser.getEmail());
            }
            if (StringUtils.isBlank(dingTalkUser.getMobile())) {
                user.setPhone(dingTalkUser.getMobile());
            }

            user.setUpdateTime(System.currentTimeMillis());
            userBaseMapper.updateById(user);

            // 更新头像
            UserExtend userExtend = new UserExtend();
            userExtend.setId(enableUser.getId());
            userExtend.setAvatar(dingTalkUser.getAvatarUrl());
            userExtendBaseMapper.update(userExtend);

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(enableUser.getId());
            //密码没实际用途，用默认密码即可
            String password = Optional.ofNullable(enableUser.getPhone())
                    .filter(StringUtils::isNotBlank)
                    .map(phone -> {
                        try {
                            // 确保电话号码长度足够进行截取
                            if (phone.length() >= 6) {
                                return CodingUtils.md5(phone.substring(phone.length() - 6));
                            }
                            // 处理电话号码长度不足6位的情况
                            return CodingUtils.md5(phone);
                        } catch (Exception e) {
                            // 如果截取过程发生异常，回退到使用完整电话号码
                            return CodingUtils.md5(phone);
                        }
                    })
                    .orElseGet(() -> CodingUtils.md5(enableUser.getLastOrganizationId() + enableUser.getId()));
            loginRequest.setPassword(password);

            loginRequest.setPlatform(DepartmentConstants.DINGTALK.name());
            loginRequest.setAuthenticate(type);
            SecurityUtils.getSubject().getSession().setAttribute("authenticate", type);
            return userLoginService.login(loginRequest);
        } else {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
    }


    public SessionUser exchangeWeComCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new AuthenticationException(Translator.get("code_not_exist"));
        }
        IntegrationConfigService integrationConfigService = CommonBeanFactory.getBean(IntegrationConfigService.class);
        //获取配置信息  //TODO: 这里待定。目前只有一个组织，按照一个组织逻辑选默认先取第一个组织
        List<ThirdConfigurationDTO> synOrganizationConfigs = integrationConfigService.getThirdConfig(DEFAULT_ORGANIZATION_ID);
        if (CollectionUtils.isEmpty(synOrganizationConfigs)) {
            throw new AuthenticationException(Translator.get("third.config.not.exist"));
        }
        ThirdConfigurationDTO weComConfig = synOrganizationConfigs.stream().filter(config -> StringUtils.equalsIgnoreCase(config.getType(), DepartmentConstants.WECOM.name())).toList().getFirst();
        if (weComConfig == null) {
            throw new AuthenticationException(Translator.get("third.config.not.exist"));
        }
        if (!weComConfig.getQrcodeEnable()) {
            throw new AuthenticationException(Translator.get("third.config.un.enable"));

        }
        return getWeComSessionUser(code, weComConfig, UserSource.QR_CODE.toString());
    }

    public SessionUser exchangeDingTalkCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new GenericException(Translator.get("code_not_exist"));
        }
        IntegrationConfigService integrationConfigService = CommonBeanFactory.getBean(IntegrationConfigService.class);
        //获取配置信息  //TODO: 这里待定。目前只有一个组织，按照一个组织逻辑选默认先取第一个组织
        List<ThirdConfigurationDTO> synOrganizationConfigs = integrationConfigService.getThirdConfig(DEFAULT_ORGANIZATION_ID);
        if (CollectionUtils.isEmpty(synOrganizationConfigs)) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }
        ThirdConfigurationDTO dingTalkConfig = synOrganizationConfigs.stream().filter(config -> StringUtils.equalsIgnoreCase(config.getType(), DepartmentConstants.DINGTALK.name())).toList().getFirst();
        if (dingTalkConfig == null) {
            throw new GenericException(Translator.get("third.config.not.exist"));
        }
        if (!dingTalkConfig.getQrcodeEnable()) {
            throw new GenericException(Translator.get("third.config.un.enable"));

        }
        return getDingTalkSessionUser(code, dingTalkConfig, UserSource.QR_CODE.toString());
    }
}
