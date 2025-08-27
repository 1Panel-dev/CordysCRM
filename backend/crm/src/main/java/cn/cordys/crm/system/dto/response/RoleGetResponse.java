package cn.cordys.crm.system.dto.response;

import cn.cordys.common.permission.PermissionDefinitionItem;
import cn.cordys.crm.system.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

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

    @Schema(description = "指定部门权限时，部门的ID")
    private List<String> deptIds;

    @Schema(description = "权限配置")
    private List<PermissionDefinitionItem> permissions;
}
