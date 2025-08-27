package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.system.dto.convert.UserRoleConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
@Data
public class RoleUserListResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "部门id")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "角色列表")
    private List<UserRoleConvert> roles;
}
