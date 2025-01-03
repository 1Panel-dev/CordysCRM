package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * 角色
 *
 * @author jianxing
 * @date 2025-01-03 09:54:59
 */
@Data
public class RoleAddRequest {
    @Max(value = 255)
    @NotBlank
    @Schema(description = "角色名称")
    private String name;

    @Max(value = 30)
    @NotBlank
    @Schema(description = "数据范围（全部数据权限/指定部门权限/本部门数据权限/本部门及以下数据权限/仅本人数据）")
    private String dataScope;

    @Max(value = 1000)
    @Schema(description = "描述")
    private String description;

    @Max(value = 32)
    @NotBlank
    @Schema(description = "组织id")
    private String organizationId;
}