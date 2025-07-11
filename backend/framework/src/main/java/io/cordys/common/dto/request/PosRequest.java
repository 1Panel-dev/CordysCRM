package io.cordys.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author guoyuqi
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PosRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "组织id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{system.org_id.not_blank}")
    private String orgId;

    @Schema(description = "移动用例id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{system.move_id.not_blank}")
    private String moveId;

    @Schema(description = "目标用例id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{system.target_id.not_blank}")
    private String targetId;

    @Schema(description = "移动类型", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"BEFORE", "AFTER", "APPEND"})
    @NotBlank(message = "{system.move_mode.not_blank}")
    private String moveMode;
}
