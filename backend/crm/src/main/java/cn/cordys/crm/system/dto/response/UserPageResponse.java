package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.system.dto.convert.UserRoleConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class UserPageResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "性别(0-男/1-女)")
    private Boolean gender;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门id")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "直属上级")
    private String supervisorId;

    @Schema(description = "直属上级名称")
    private String supervisorName;

    @Schema(description = "角色列表")
    private List<UserRoleConvert> roles;

    @Schema(description = "工号")
    private String employeeId;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "员工类型")
    private String employeeType;

    @Schema(description = "工作城市")
    private String workCity;

    @Schema(description = "用户组")
    private List<UserRoleConvert> userGroups;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "修改人名称")
    private String updateUserName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "是否是负责人")
    private Boolean commander;

    @Schema(description = "入职时间")
    private Long onboardingDate;

}
