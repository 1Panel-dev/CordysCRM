package cn.cordys.crm.integration.sso.service;

import cn.cordys.common.util.EncryptUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.agent.constant.MaxKBApiPaths;
import cn.cordys.crm.integration.agent.response.MaxKBResponseEntity;
import cn.cordys.crm.integration.common.client.QrCodeClient;
import cn.cordys.crm.integration.common.utils.HttpRequestUtil;
import cn.cordys.crm.integration.dingtalk.constant.DingTalkApiPaths;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkBaseParamDTO;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkSendDTO;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkToken;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkTokenParamDTO;
import cn.cordys.crm.integration.lark.constant.LarkApiPaths;
import cn.cordys.crm.integration.lark.dto.LarkBaseParamDTO;
import cn.cordys.crm.integration.lark.dto.LarkSendMessageDTO;
import cn.cordys.crm.integration.lark.dto.LarkToken;
import cn.cordys.crm.integration.lark.dto.LarkTokenParamDTO;
import cn.cordys.crm.integration.qcc.constant.QccApiPaths;
import cn.cordys.crm.integration.tender.constant.TenderApiPaths;
import cn.cordys.crm.integration.wecom.constant.WeComApiPaths;
import cn.cordys.crm.integration.wecom.dto.WeComDetail;
import cn.cordys.crm.integration.wecom.dto.WeComSendDTO;
import cn.cordys.crm.integration.wecom.dto.WeComToken;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.cordys.crm.integration.dingtalk.constant.DingTalkApiPaths.DING_USER_TOKEN_URL;

@Service
@Transactional(rollbackFor = Exception.class)
public class TokenService {

    @Resource
    private QrCodeClient qrCodeClient;

    /**
     * 获取assess_Token
     *
     * @param corpId     企业ID
     * @param corpSecret 企业应用 密钥
     *
     * @return String token
     */
    public String getAssessToken(String corpId, String corpSecret) {
        String url = HttpRequestUtil.urlTransfer(WeComApiPaths.GET_TOKEN, corpId, corpSecret);
        WeComToken weComToken = new WeComToken();
        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            weComToken = JSON.parseObject(response, WeComToken.class);

        } catch (Exception e) {
            LogUtils.error(Translator.get("auth.get.token.error"), e);
        }

        if (weComToken.getErrCode() == null) {
            LogUtils.error(Translator.get("auth.get.token.res.error"));
            return null;
        }

        if (weComToken.getErrCode() != 0) {
            LogUtils.error(Translator.get("auth.get.token.res.error") + ":" + weComToken.getErrMsg());
            return null;
        }
        return weComToken.getAccessToken();
    }


    /**
     * 获取DingTalk assess_Token
     *
     * @param appKey    企业应用 ID ClientId
     * @param appSecret 企业应用 密钥
     *
     * @return String token
     */
    public String getDingTalkToken(String appKey, String appSecret) {

        DingTalkBaseParamDTO dingTalkTokenParamDTO = new DingTalkBaseParamDTO();
        dingTalkTokenParamDTO.setAppKey(appKey);
        dingTalkTokenParamDTO.setAppSecret(appSecret);
        DingTalkToken dingTalkToken = new DingTalkToken();
        try {
            String response = qrCodeClient.postExchange(
                    DingTalkApiPaths.DING_TALK_GET_TOKEN, null, null, dingTalkTokenParamDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON
            );
            dingTalkToken = JSON.parseObject(response, DingTalkToken.class);
        } catch (Exception e) {
            LogUtils.error(Translator.get("auth.get.token.error"), e);
        }

        return dingTalkToken.getAccessToken();
    }


    /**
     * 获取DingTalk用户 assess_Token
     *
     * @param appKey    企业应用 ID ClientId 应用id。可使用扫码登录应用或者第三方个人小程序的appId。
     * @param appSecret 企业应用 密钥
     * @param code      授权码OAuth 2.0 临时授权码
     *
     * @return String token
     */
    public String getDingTalkUserToken(String appKey, String appSecret, String code) {

        DingTalkTokenParamDTO dingTalkTokenParamDTO = new DingTalkTokenParamDTO();
        dingTalkTokenParamDTO.setClientId(appKey);
        dingTalkTokenParamDTO.setClientSecret(appSecret);
        dingTalkTokenParamDTO.setCode(code);
        dingTalkTokenParamDTO.setGrantType("authorization_code");
        DingTalkToken dingTalkToken = new DingTalkToken();
        try {
            String response = qrCodeClient.postExchange(DING_USER_TOKEN_URL, null, null, dingTalkTokenParamDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
            dingTalkToken = JSON.parseObject(response, DingTalkToken.class);
        } catch (Exception e) {
            LogUtils.error(Translator.get("auth.get.token.error"), e);
        }

        return dingTalkToken.getAccessToken();
    }


    /**
     * 获取Lark token
     *
     * @param agentId   appId 飞书自建应用凭证
     * @param appSecret appSecret
     *
     * @return tenantAccessToken
     */
    public String getLarkToken(String agentId, String appSecret) {

        LarkBaseParamDTO larkBaseParamDTO = new LarkBaseParamDTO();
        larkBaseParamDTO.setApp_id(agentId);
        larkBaseParamDTO.setApp_secret(appSecret);

        LarkToken larkToken = new LarkToken();

        try {
            String response = qrCodeClient.postExchange(
                    LarkApiPaths.LARK_APP_TOKEN_URL, null, null, larkBaseParamDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON
            );
            larkToken = JSON.parseObject(response, LarkToken.class);

        } catch (Exception e) {
            LogUtils.error(Translator.get("auth.get.token.error"), e);
        }

        if (larkToken.getCode() != 0) {
            LogUtils.error(Translator.get("auth.get.token.res.error") + ":" + larkToken.getMsg());
        }

        return larkToken.getTenantAccessToken();
    }

    /**
     * IP + 端口 是否连通
     *
     * @param fullUrl 完整的URL地址
     *
     * @return bool
     */
    public boolean pingDeUrl(String fullUrl) {
        try {
            // 禁用系统代理设置
            System.setProperty("java.net.useSystemProxies", "false");

            URL url = URI.create(fullUrl).toURL();
            String host = url.getHost();
            // 根据协议选择默认端口
            int defaultPort = "https".equalsIgnoreCase(url.getProtocol()) ? 443 : 80;
            int port = url.getPort() != -1 ? url.getPort() : defaultPort;

            // 直接测试连接
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 3000);
            }
        } catch (Exception e) {
            LogUtils.error("de embedded url connect error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 获取sqlBot的src 是否能连通
     */
    public boolean getSqlBotSrc(String scriptCode) {
        Pattern pattern = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(scriptCode);

        String jsUrl;
        if (matcher.find()) {
            jsUrl = matcher.group(1);
        } else {
            return false;
        }
        try {
            URL url = URI.create(jsUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // 更轻量，只请求头部
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            LogUtils.error(e);
            return false;
        }

    }

    /**
     * @param code   code
     * @param config 认证配置的map
     *
     * @return access_token
     */
    public String getGitHubOAuth2Token(String code, Map<String, String> config) {
        String url = config.get("tokenUrl")
                + "?client_id=" + config.get("clientId")
                + "&client_secret=" + config.get("secret")
                + "&redirect_uri=" + config.get("redirectUrl")
                + "&code=" + code
                + "&grant_type=authorization_code";

        Map<String, String> resultObj = null;
        try {
            String credentials = EncryptUtils.base64Encoding(config.get("clientId") + ":" + config.get("secret"));
            String content = qrCodeClient.postExchange(url, "Basic " + credentials, HttpHeaders.AUTHORIZATION, HttpEntity.EMPTY, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
            resultObj = JSON.parseObject(content, new TypeReference<HashMap<String, String>>() {
            });
        } catch (Exception e) {
            LogUtils.error(Translator.get("auth.token.error"));

        }
        String accessToken = null;
        if (MapUtils.isEmpty(resultObj)) {
            LogUtils.error(Translator.get("auth.token.error"));
        } else {
            accessToken = resultObj.get("access_token");
        }
        if (StringUtils.isBlank(accessToken)) {
            LogUtils.error(Translator.get("auth.token.error"));
        }

        return accessToken;
    }

    public void sendNoticeByToken(WeComSendDTO weComSendDTO, String corpId, String appSecret) {
        String assessToken = getAssessToken(corpId, appSecret);
        String detailUrl = HttpRequestUtil.urlTransfer(WeComApiPaths.SEND_INFO, assessToken);
        qrCodeClient.postExchange(detailUrl, null, null, weComSendDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
    }

    public void sendDingNoticeByToken(DingTalkSendDTO dingTalkSendDTO, String agentId, String appSecret) {
        String assessToken = getDingTalkToken(agentId, appSecret);
        String detailUrl = HttpRequestUtil.urlTransfer(DingTalkApiPaths.DING_NOTICE_URL, assessToken);
        qrCodeClient.postExchange(detailUrl, null, null, dingTalkSendDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
    }

    /**
     * 发送Lark通知通过token
     *
     * @param agentId            appId 飞书自建应用凭证
     * @param appSecret          appSecret 飞书自建应用密钥
     * @param larkSendMessageDTO 消息体
     */
    public void sendLarkNoticeByToken(LarkSendMessageDTO larkSendMessageDTO, String agentId, String appSecret) {
        String assessToken = getLarkToken(agentId, appSecret);
        qrCodeClient.postExchange(LarkApiPaths.LARK_SEND_MESSAGE_URL + "?receive_id_type=" + "open_id", "Bearer " + assessToken,
                "Authorization", larkSendMessageDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
    }


    public String getLarkUserAccessToken(String agentId, String appSecret, String redirectUrl, String code) {
        LarkTokenParamDTO larkTokenParamDTO = new LarkTokenParamDTO();
        larkTokenParamDTO.setClient_id(agentId);
        larkTokenParamDTO.setClient_secret(appSecret);
        larkTokenParamDTO.setCode(code);
        larkTokenParamDTO.setGrant_type("authorization_code");
        larkTokenParamDTO.setRedirect_uri(redirectUrl);
        LarkToken larkToken = new LarkToken();

        try {
            String response = qrCodeClient.postExchange(
                    LarkApiPaths.LARK_USER_TOKEN_URL, null, null, larkTokenParamDTO, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON
            );
            larkToken = JSON.parseObject(response, LarkToken.class);

        } catch (Exception e) {
            LogUtils.error(Translator.get("auth.get.token.error"), e);
        }

        if (larkToken.getCode() != 0) {
            LogUtils.error(Translator.get("auth.get.token.res.error") + ":" + larkToken.getMsg());
        }

        return larkToken.getAccessToken();
    }


    /**
     * 测试连接maxkb
     *
     * @param mkAddress
     * @param apiKey
     *
     * @return
     */
    public Boolean getMaxKBToken(String mkAddress, String apiKey) {
        String body = qrCodeClient.exchange(
                HttpRequestUtil.urlTransfer(mkAddress.concat(MaxKBApiPaths.APPLICATION), "default"),
                "Bearer " + apiKey,
                HttpHeaders.AUTHORIZATION,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON
        );
        MaxKBResponseEntity entity = JSON.parseObject(body, MaxKBResponseEntity.class);
        return entity != null && entity.getCode() == 200;
    }


    public Boolean getTender() {
        try {
            URL url = URI.create(TenderApiPaths.TENDER_API).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.connect();
            return true;
        } catch (Exception e) {
            LogUtils.error(e);
            return false;
        }

    }

    public boolean getQcc(String qccAddress, String qccAccessKey, String qccSecretKey) {

        long time = System.currentTimeMillis() / 1000;
        String token = EncryptUtils.md5(qccAccessKey + time + qccSecretKey).toUpperCase();

        Map<String, String> headers = new HashMap<>();
        headers.put("Token", token);
        headers.put("Timespan", String.valueOf(time));

        try {

            String url = HttpRequestUtil.urlTransfer(qccAddress.concat(QccApiPaths.FUZZY_SEARCH_API), qccAccessKey, qccSecretKey);
            HttpRequestUtil.sendGetRequest(url, headers);
            return true;
        } catch (Exception e) {
            LogUtils.error("测试连接失败", e);
            return false;
        }
    }

    public boolean checkWeComAgentAvailable(String accessToken, String agentId) {
        String url = HttpRequestUtil.urlTransfer(
                WeComApiPaths.GET_AGENT, accessToken, agentId
        );

        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            WeComDetail weComDetail = JSON.parseObject(response, WeComDetail.class);

            LogUtils.info(
                    "企业微信 Agent 连接测试结果：errCode={}, errMsg={}",
                    weComDetail.getErrCode(),
                    weComDetail.getErrMsg()
            );

            return weComDetail.getErrCode() != null && weComDetail.getErrCode() == 0;
        } catch (Exception e) {
            LogUtils.error("企业微信 Agent 连接测试异常", e);
            return false;
        }
    }
}
