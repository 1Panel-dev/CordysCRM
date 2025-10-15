package cn.cordys.crm.integration.dataease.service;

import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.dataease.dto.DeAuthDTO;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DataEaseService {
    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;
    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    /**
     * 获取嵌入式DE-Token
     */
    public DeAuthDTO getEmbeddedDeToken(String organizationId, String userId, boolean isModule) {
        // 获取组织配置
        ThirdConfigurationDTO thirdConfig = getDeConfig(organizationId);
        String account = thirdConfig.getDeAccount();
        if (BooleanUtils.isTrue(isModule)) {
            account = userId;
        }

        return getEmbeddedDeToken(account, thirdConfig);
    }

    public DeAuthDTO getEmbeddedDeToken(String account, ThirdConfigurationDTO thirdConfig) {
        // 生成token
        String token = generateJwtToken(
                thirdConfig.getAgentId(),
                thirdConfig.getAppSecret(),
                account
        );

        return DeAuthDTO.builder()
                .token(token)
                .url(thirdConfig.getRedirectUrl())
                .build();
    }

    public ThirdConfigurationDTO getDeConfig(String organizationId) {
        OrganizationConfig config =
                extOrganizationConfigMapper.getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());

        // 获取DE配置详情
        List<OrganizationConfigDetail> configDetails =
                extOrganizationConfigDetailMapper.getOrgConfigDetailByType(config.getId(), null,
                        List.of(ThirdConstants.ThirdDetailType.DE_BOARD.toString()));

        OrganizationConfigDetail configDetail = configDetails.getFirst();

        // 解析配置
        return JSON.parseObject(
                new String(configDetail.getContent()), ThirdConfigurationDTO.class
        );
    }

    /**
     * 生成JWT-Token
     *
     * @param appId     应用ID
     * @param appSecret 应用密钥
     *
     * @return JWT Token
     */
    private String generateJwtToken(String appId, String appSecret, String account) {
        Algorithm algorithm = Algorithm.HMAC256(appSecret);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("account", account).withClaim("appId", appId);
        builder.withIssuedAt(new Date());
        return builder.sign(algorithm);
    }
}
