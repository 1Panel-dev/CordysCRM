package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WebHookConfig {


    @Schema(description = "是否启用")
    private Boolean webHookEnable;

    @Schema(description = "webhook地址")
    private String webHookUrl;

    @Schema(description = "请求方式")
    private String webHookMethod;

    @Schema(description = "请求头")
    private String webHookHeader;

    @Schema(description = "请求体")
    private String webHookBody;

    @Schema(description = "说明")
    private String webHookDescribe;


    public WebHookConfig() {
    }
}