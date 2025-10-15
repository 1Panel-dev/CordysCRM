package cn.cordys.crm.integration.common.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.client.QrCodeClient;
import cn.cordys.crm.integration.common.utils.HttpRequestUtil;
import cn.cordys.crm.integration.dingtalk.constant.DingTalkApiPaths;
import cn.cordys.crm.integration.dingtalk.response.DingTalkUserResponse;
import cn.cordys.crm.integration.lark.constant.LarkApiPaths;
import cn.cordys.crm.integration.wecom.constant.WeComApiPaths;
import cn.cordys.crm.integration.wecom.dto.WeComUserTicketDTO;
import cn.cordys.crm.integration.wecom.response.WeComCommonUserResponse;
import cn.cordys.crm.integration.wecom.response.WeComUserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
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
     *
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
     *
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
     *
     * @return DingTalkUserResponse
     */
    public DingTalkUserResponse getDingTalkUser(String dingToken) {
        return exchangeWithWrap(
                dingToken
        );
    }

    /**
     * 根据unionid获取钉钉用户id
     *
     * @param accessToken access_token
     * @param unionid     unionid
     * @return userId
     */
    public String getUserIdByUnionId(String accessToken, String unionid) {
        String url = HttpRequestUtil.urlTransfer(DingTalkApiPaths.DING_USERID_BY_UNIONID, accessToken);
        Map<String, String> body = new HashMap<>();
        body.put("unionid", unionid);
        String response = post(url, body);
        Map<String, Object> result = JSON.parseObject(response, new TypeReference<>() {});
        if (result.get("errcode") != null && (Integer) result.get("errcode") == 0) {
            Map<String, Object> user = (Map<String, Object>) result.get("result");
            return (String) user.get("userid");
        } else {
            throw new GenericException("Error getting user id from unionid: " + result.get("errmsg"));
        }
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

    private String post(String url, Object body) {
        try {
            return qrCodeClient.postExchange(
                    url, null, null,
                    body,
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
    }

    private WeComUserResponse postWithWrap(String url, Object body) {
        try {
            String response = qrCodeClient.postExchange(
                    url, null, null,
                    body,
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );
            return JSON.parseObject(response, WeComUserResponse.class);
        } catch (Exception e) {
            throw new GenericException(Translator.get("auth.get.user.error"));
        }
    }

    private DingTalkUserResponse exchangeWithWrap(String token) {
        try {
            String body = qrCodeClient.exchange(
                    DingTalkApiPaths.DING_USER_INFO,
                    token,
                    "x-acs-dingtalk-access-token",
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );
            return JSON.parseObject(body, DingTalkUserResponse.class);
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

    public Map<String, Object> getLarkUser(String assessToken) {
        String body = qrCodeClient.exchange(
                LarkApiPaths.LARK_USER_INFO_URL,
                "Bearer " + assessToken,
                HttpHeaders.AUTHORIZATION,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON
        );
        return JSON.parseObject(body, new TypeReference<HashMap<String, Object>>() {
        });
    }
}