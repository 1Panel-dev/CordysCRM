package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 状态权限配置DTO
 */
@Data
public class StatusPermissionDTO {
    @Schema(description = "审批状态")
    private String approvalStatus;

    @Schema(description = "权限")
    private String permission;

    @Schema(description = "是否启用")
    private Boolean enabled;
}