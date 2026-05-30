package cn.cordys.crm.integration.lark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LarkTokenParamDTO extends LarkBaseParamDTO {

    @Schema(description = "code")
    public String code;
    @Schema(description = "grantType")
    public String grant_type;
    @Schema(description = "clientId")
    public String client_id;
    @Schema(description = "clientSecret")
    public String client_secret;
    @Schema(description = "redirect_uri")
    public String redirect_uri;

}
