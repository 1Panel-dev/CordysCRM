package cn.cordys.crm.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderUpdateRequest extends OrderAddRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Schema(description = "是否提审更新  normal-正常更新  approval-评审更新")
    private String updateType;

    @Schema(description = "变更说明")
    private String comment;
}
