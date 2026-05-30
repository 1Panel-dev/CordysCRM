package cn.cordys.crm.clue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PoolClueAssignRequest {

    @NotBlank
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clueId;

    @NotBlank
    @Schema(description = "分配用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assignUserId;
}
