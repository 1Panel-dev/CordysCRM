package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.groups.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class SysUser extends BaseModel {
    @Size(min = 1, max = 255, message = "{sys_user.name.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_user.name.not_blank}", groups = {Created.class, Updated.class})
    private String name;

    @Size(min = 1, max = 11, message = "{sys_user.phone.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_user.phone.not_blank}", groups = {Created.class, Updated.class})
    private String phone;

    @Size(min = 1, max = 255, message = "{sys_user.email.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_user.email.not_blank}", groups = {Created.class, Updated.class})
    private String email;

    @Size(min = 1, max = 255, message = "{sys_user.password.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_user.password.not_blank}", groups = {Created.class, Updated.class})
    private String password;

    @NotBlank(message = "{sys_user.enable.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private int enable;

    @Schema(description = "工号")
    private String employeeId;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "员工类型")
    private String employeeType;

    @Schema(description = "直属上级")
    private String supervisorId;

    @Schema(description = "工作城市")
    private String workCity;
}