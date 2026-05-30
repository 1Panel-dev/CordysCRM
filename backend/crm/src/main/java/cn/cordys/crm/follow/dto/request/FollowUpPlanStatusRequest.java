package cn.cordys.crm.follow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FollowUpPlanStatusRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;


    @Schema(description = "状态: PREPARED/UNDERWAY/COMPLETED/CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

}
