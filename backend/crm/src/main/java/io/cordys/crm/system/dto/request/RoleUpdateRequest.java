package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-01-03 10:48:10
 */
@Data
public class RoleUpdateRequest {

    @NotBlank
    @Max(value = 32)
    @Schema(description = "id")
    private String id;

    @Max(value = 255)
    @Schema(description = "角色名称")
    private String name;

    @Max(value = 30)
    @Schema(description = "数据范围（全部数据权限/指定部门权限/本部门数据权限/本部门及以下数据权限/仅本人数据）")
    private String dataScope;

    @Max(value = 1000)
    @Schema(description = "描述")
    private String description;
}