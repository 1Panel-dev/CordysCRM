package io.cordys.crm.integration.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.integration.auth.client.QrCodeClient;
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

import static io.cordys.crm.integration.dingtalk.constant.DingTalkApiPaths.DING_USER_INFO;

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
        String body = qrCodeClient.exchange(userInfoUrl, "Bearer " + accessToken, HttpHeaders.AUTHORIZATION, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
        return JSON.parseObject(body, new TypeReference<HashMap<String, Object>>() {
        });
    }


    /**
     * 获取企业微信访问用户身份
     *
     * @param assessToken token
     * @return WeComUser
     */
    public WeComUserResponse getWeComUser(String assessToken, String code) {
        String url = HttpRequestUtil.urlTransfer(WeComApiPaths.USER_ID_TICKET, assessToken, code);
        WeComCommonUserResponse weComCommonUserResponse = new WeComCommonUserResponse();
        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            weComCommonUserResponse = JSON.parseObject(response, WeComCommonUserResponse.class);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }

        if (weComCommonUserResponse.getErrCode() == null) {
            throw new GenericException(Translator.get("auth.get.user.res.error"));
        }

        if (weComCommonUserResponse.getErrCode() != 0) {
            throw new GenericException(Translator.get("auth.get.user.res.error") + ":" + weComCommonUserResponse.getErrMsg());
        }
        WeComUserResponse weComUserResponse = new WeComUserResponse();
        String userTicket = weComCommonUserResponse.getUserTicket();
        if (StringUtils.isNotBlank(userTicket)) {
            String detailUrl = HttpRequestUtil.urlTransfer(WeComApiPaths.USER_DETAIL, assessToken);
            WeComUserTicketDTO userTicketDTO = new WeComUserTicketDTO();
            userTicketDTO.setUser_ticket(userTicket);
            try {
                String response = qrCodeClient.postExchange(
                        detailUrl, null, null, userTicketDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON
                );
                weComUserResponse = JSON.parseObject(response, WeComUserResponse.class);
            } catch (Exception e) {
                throw new GenericException(Translator.get("auth.get.user.error"));
            }
        } else {
            String detailUrl = HttpRequestUtil.urlTransfer(WeComApiPaths.USER_INFO, assessToken, weComCommonUserResponse.getUserId());
            try {
                String response = HttpRequestUtil.sendGetRequest(detailUrl, null);
                weComUserResponse = JSON.parseObject(response, WeComUserResponse.class);

            } catch (Exception e) {
                throw new GenericException(Translator.get("auth.get.user.error"));
            }
        }

        if (weComUserResponse.getErrCode() == null) {
            throw new GenericException(Translator.get("auth.get.user.res.error"));
        }

        if (weComUserResponse.getErrCode() != 0) {
            throw new GenericException(Translator.get("auth.get.user.res.error") + ":" + weComUserResponse.getErrMsg());
        }

        return weComUserResponse;
    }

    /**
     * 获取钉钉用户信息
     * @param dingToken 用户token
     * @return DingTalkUserResponse
     */
    public DingTalkUserResponse getDingTalkUser(String dingToken) {
        DingTalkUserResponse dingTalkUserResponse = new DingTalkUserResponse();
        try {
            String body = qrCodeClient.exchange(DING_USER_INFO, dingToken, "x-acs-dingtalk-access-token", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
            dingTalkUserResponse = JSON.parseObject(body, DingTalkUserResponse.class);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
        return dingTalkUserResponse;
    }


}
