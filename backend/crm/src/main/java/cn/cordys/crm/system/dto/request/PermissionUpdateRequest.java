package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianxing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionUpdateRequest {
    @NotBlank
    @Schema(description = "权限ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;
    @Schema(description = "是否启用该权限", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enable = false;
}
