package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.system.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
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
