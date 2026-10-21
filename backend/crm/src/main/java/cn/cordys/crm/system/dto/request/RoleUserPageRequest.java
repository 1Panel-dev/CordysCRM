package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class RoleUserPageRequest extends BasePageRequest {
    @NotBlank
    @Schema(description = "角色ID")
    private String roleId;
}
