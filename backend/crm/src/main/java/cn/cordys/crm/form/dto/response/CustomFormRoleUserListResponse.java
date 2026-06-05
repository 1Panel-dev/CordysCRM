package cn.cordys.crm.form.dto.response;

import cn.cordys.crm.system.dto.convert.UserRoleConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomFormRoleUserListResponse {

    @Schema(description = "自定义表单角色用户关联ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "部门ID")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "角色列表")
    private List<UserRoleConvert> roles;
}
