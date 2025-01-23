package io.cordys.crm.system.dto.request;

import io.cordys.common.constants.EnumValue;
import io.cordys.common.constants.RoleDataScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-01-03 10:48:10
 */
@Data
public class RoleUpdateRequest {

    @NotBlank
    @Size(max = 32)
    @Schema(description = "id")
    private String id;

    @Size(max = 255)
    @Schema(description = "角色名称")
    private String name;

    @Size(max = 30)
    @Schema(description = "数据范围（全部数据权限/指定部门权限/本部门数据权限/本部门及以下数据权限/仅本人数据）")
    @EnumValue(enumClass = RoleDataScope.class)
    private String dataScope;

    @Schema(description = "指定部门权限时，部门的ID")
    private List<String> deptIds;

    @Schema(description =  "菜单下的权限列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<PermissionUpdateRequest> permissions;

    @Size(max = 1000)
    @Schema(description = "描述")
    private String description;
}