package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
@Data
public class RoleUserPageRequest extends BasePageRequest {
    @NotBlank
    @Schema(description = "角色ID")
    private String roleId;
}
