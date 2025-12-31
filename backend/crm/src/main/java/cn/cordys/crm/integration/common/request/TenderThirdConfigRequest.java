package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TenderThirdConfigRequest {

    @Schema(description = "tender开启")
    private Boolean tenderEnable;

    @Schema(description = "地址")
    private String tenderAddress;
}
