package cn.cordys.crm.system.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.common.constants.RoleDataScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 * 角色
 *
 * @author jianxing
 * @date 2025-01-03 09:54:59
 */
@Data
public class RoleAddRequest {
    @Size(max = 255)
    @NotBlank
    @Schema(description = "角色名称")
    private String name;

    @Size(max = 30)
    @Schema(description = "数据范围（全部数据权限/指定部门权限/本部门数据权限/本部门及以下数据权限/仅本人数据）")
    @EnumValue(enumClass = RoleDataScope.class)
    private String dataScope;

    @Schema(description = "指定部门权限时，部门的ID")
    private List<String> deptIds;

    @Schema(description = "菜单下的权限列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<PermissionUpdateRequest> permissions;

    @Size(max = 1000)
    @Schema(description = "描述")
    private String description;
}