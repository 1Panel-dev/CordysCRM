package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchAffectReasonResponse {

    @Schema(description = "成功数量")
    private Integer success;

    @Schema(description = "失败数量")
    private Integer fail;

    @Schema(description = "失败原因")
    private String errorMessages;

}
