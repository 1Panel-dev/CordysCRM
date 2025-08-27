package cn.cordys.crm.integration.wecom.dto;

import cn.cordys.crm.integration.wecom.response.WeComResponseEntity;
import lombok.Data;

@Data
public class WeComAgentDetail extends WeComResponseEntity {
    /**
     * 应用id
     */
    private String agentId;

    /**
     * 应用名称
     */
    private String name;

    /**
     * 企业应用方形头像
     */
    private String square_logo_url;

    /**
     * 应用描述
     */
    private String description;

}
