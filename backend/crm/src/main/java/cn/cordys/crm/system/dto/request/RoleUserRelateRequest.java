package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
@Data
public class RoleUserRelateRequest {
    @NotBlank
    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "部门ID")
    private List<String> deptIds;

    @Schema(description = "角色ID")
    private List<String> roleIds;

    @Schema(description = "用户ID")
    private List<String> userIds;
}
