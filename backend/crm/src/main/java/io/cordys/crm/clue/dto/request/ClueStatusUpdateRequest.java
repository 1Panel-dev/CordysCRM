package io.cordys.crm.clue.dto.request;

import io.cordys.common.constants.EnumValue;
import io.cordys.crm.clue.constants.ClueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueStatusUpdateRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Size(max = 30)
    @EnumValue(enumClass = ClueStatus.class)
    @Schema(description = "线索状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}