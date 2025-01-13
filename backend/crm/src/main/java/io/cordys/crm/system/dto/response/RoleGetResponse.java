package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import io.cordys.crm.system.domain.Role;

/**
 *
 * @author jianxing
 * @date 2025-01-10 18:35:02
 */
@Data
public class RoleGetResponse extends Role {
    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;
}
