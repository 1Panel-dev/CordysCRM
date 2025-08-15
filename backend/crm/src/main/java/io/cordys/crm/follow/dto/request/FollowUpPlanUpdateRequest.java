package io.cordys.crm.follow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FollowUpPlanUpdateRequest extends FollowUpPlanAddRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @NotNull
    @Schema(description = "是否转为跟进记录", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean converted;
}
