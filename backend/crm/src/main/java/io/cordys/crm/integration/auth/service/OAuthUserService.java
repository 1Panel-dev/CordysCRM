package io.cordys.crm.integration.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.integration.auth.client.QrCodeClient;
import io.cordys.crm.integration.dingtalk.constant.DingTalkApiPaths;
import io.cordys.crm.integration.dingtalk.response.DingTalkUserResponse;
import io.cordys.crm.integration.wecom.constant.WeComApiPaths;
import io.cordys.crm.integration.wecom.dto.WeComUserTicketDTO;
import io.cordys.crm.integration.wecom.response.WeComCommonUserResponse;
import io.cordys.crm.integration.wecom.response.WeComUserResponse;
import io.cordys.crm.integration.wecom.utils.HttpRequestUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class OAuthUserService {

    @Resource
    private QrCodeClient qrCodeClient;

    /**
     * 获取github Oauth2 用户信息
     *
     * @param userInfoUrl 获取用户信息url
     * @param accessToken token
     * @return Map<String, Object>
     */
    public Map<String, Object> getGitHubUser(String userInfoUrl, String accessToken) {
        String body = qrCodeClient.exchange(
                userInfoUrl,
                "Bearer " + accessToken,
                HttpHeaders.AUTHORIZATION,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON
        );
        return JSON.parseObject(body, new TypeReference<HashMap<String, Object>>() {
        });
    }

    /**
     * 获取企业微信访问用户身份
     *
     * @param assessToken token
     * @param code        code
     * @return WeComUserResponse
     */
    public WeComUserResponse getWeComUser(String assessToken, String code) {
        // 1. 根据 code 获取通用用户信息
        String commonUrl = HttpRequestUtil.urlTransfer(WeComApiPaths.USER_ID_TICKET, assessToken, code);
        WeComCommonUserResponse commonRes = getWithWrap(commonUrl, WeComCommonUserResponse.class);
        validateWeCom(commonRes.getErrCode(), commonRes.getErrMsg());

        // 2. 根据是否有 user_ticket 获取详细信息
        WeComUserResponse userRes;
        String userTicket = commonRes.getUserTicket();
        if (StringUtils.isNotBlank(userTicket)) {
            String detailUrl = HttpRequestUtil.urlTransfer(WeComApiPaths.USER_DETAIL, assessToken);
            WeComUserTicketDTO body = new WeComUserTicketDTO();
            body.setUser_ticket(userTicket);
            userRes = postWithWrap(detailUrl, body);
        } else {
            String detailUrl = HttpRequestUtil.urlTransfer(WeComApiPaths.USER_INFO, assessToken, commonRes.getUserId());
            userRes = getWithWrap(detailUrl, WeComUserResponse.class);
        }

        // 3. 校验详细信息返回
        validateWeCom(userRes.getErrCode(), userRes.getErrMsg());
        return userRes;
    }

    /**
     * 获取钉钉用户信息
     *
     * @param dingToken 用户token
     * @return DingTalkUserResponse
     */
    public DingTalkUserResponse getDingTalkUser(String dingToken) {
        return exchangeWithWrap(
                dingToken
        );
    }

    // -------------------- private helpers --------------------

    private <T> T getWithWrap(String url, Class<T> clazz) {
        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            return JSON.parseObject(response, clazz);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
    }

    private <T> T postWithWrap(String url, Object body) {
        try {
            String response = qrCodeClient.postExchange(
                    url, null, null,
                    body,
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );
            return JSON.parseObject(response, (Class<T>) WeComUserResponse.class);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
    }

    private <T> T exchangeWithWrap(String token) {
        try {
            String body = qrCodeClient.exchange(
                    DingTalkApiPaths.DING_USER_INFO,
                    token,
                    "x-acs-dingtalk-access-token",
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );
            return JSON.parseObject(body, (Class<T>) DingTalkUserResponse.class);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
    }

    private void validateWeCom(Integer errCode, String errMsg) {
        if (errCode == null) {
            throw new GenericException(Translator.get("auth.get.user.res.error"));
        }
        if (errCode != 0) {
            throw new GenericException(Translator.get("auth.get.user.res.error") + ":" + errMsg);
        }
    }
}