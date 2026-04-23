package cn.cordys.crm.approval.dto;

import cn.cordys.crm.approval.constants.FieldPermissionTypeEnum;
import cn.cordys.common.constants.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字段权限配置DTO
 */
@Data
public class FieldPermissionDTO {

    @Schema(description = "字段ID")
    private String fieldId;

    @EnumValue(enumClass = FieldPermissionTypeEnum.class)
    @Schema(description = "权限类型：HIDDEN/VIEW/EDIT")
    private String permissionType;
}