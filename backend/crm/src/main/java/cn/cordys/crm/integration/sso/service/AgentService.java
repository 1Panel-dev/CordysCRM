package cn.cordys.crm.integration.sso.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.wecom.constant.WeComApiPaths;
import cn.cordys.crm.integration.wecom.dto.WeComAgentDetail;
import cn.cordys.crm.integration.wecom.utils.HttpRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class AgentService {

    /**
     * 验证Agent信息
     *
     * @param accessToken token
     * @param agentId     企业应用id
     * @return String token
     */
    public Boolean getWeComAgent(String accessToken, String agentId) {
        String url = HttpRequestUtil.urlTransfer(WeComApiPaths.GET_AGENT, accessToken, agentId);
        WeComAgentDetail weComAgentDetail = new WeComAgentDetail();
        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            weComAgentDetail = JSON.parseObject(response, WeComAgentDetail.class);

        } catch (Exception e) {
            return false;
        }
        if (weComAgentDetail.getErrCode() == null) {
            return false;
        }
        return weComAgentDetail.getErrCode() == 0;
    }

}
