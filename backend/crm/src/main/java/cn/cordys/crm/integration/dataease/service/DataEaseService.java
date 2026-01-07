package cn.cordys.crm.integration.dataease.service;

import cn.cordys.common.constants.ThirdDetailType;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.DeThirdConfigRequest;
import cn.cordys.crm.integration.dataease.dto.DeAuthDTO;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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
        DeThirdConfigRequest thirdConfig = getDeConfig(organizationId);
        return getEmbeddedDeToken(userId, thirdConfig);
    }

    public DeAuthDTO getEmbeddedDeToken(String account, DeThirdConfigRequest thirdConfig) {
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

    public DeThirdConfigRequest getDeConfig(String organizationId) {
        var config = Optional.ofNullable(
                extOrganizationConfigMapper.getOrganizationConfig(
                        organizationId,
                        OrganizationConfigConstants.ConfigType.THIRD.name()
                )
        ).orElseThrow(() ->
                new GenericException("未找到 DataEase 相关配置")
        );

        var detail = extOrganizationConfigDetailMapper
                .getOrgConfigDetailByType(
                        config.getId(),
                        null,
                        List.of(ThirdDetailType.DE_BOARD.name())
                )
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new GenericException("未找到 DataEase 配置详情")
                );

        var baseDTO = Optional.ofNullable(
                JSON.parseObject(new String(detail.getContent()), ThirdConfigBaseDTO.class)
        ).orElseThrow(() ->
                new GenericException("DataEase 配置内容解析失败")
        );

        return JSON.MAPPER.convertValue(
                baseDTO.getConfig(),
                DeThirdConfigRequest.class
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
