package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class DepartmentSourceUserDTO implements Serializable {

    @Schema(description = "id")
    private String id;

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "三方唯一id")
    private String resourceUserId;

    @Schema(description = "部门id")
    private String departmentId;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "部门的来源id")
    private String resourceId;
}
