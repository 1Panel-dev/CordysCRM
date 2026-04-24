package cn.cordys.crm.approval.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 审批流状态权限配置响应
 */
@Data
@Schema(description = "审批流状态权限配置")
public class StatusPermissionSettingResponse implements Serializable {

    @Schema(description = "权限列表")
    private List<OptionDTO> permissions;

    @Schema(description = "状态权限配置")
    private List<StatusPermissionDTO> statusPermissions;
}
