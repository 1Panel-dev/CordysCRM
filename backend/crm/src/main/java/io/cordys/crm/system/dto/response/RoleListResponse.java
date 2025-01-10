package io.cordys.crm.system.dto.response;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import io.cordys.crm.system.domain.Role;


/**
 *
 * @author jianxing
 * @date 2025-01-09 16:46:27
 */
@Data
public class RoleListResponse extends Role {
    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;
}
