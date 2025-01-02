package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_user")
public class User extends BaseModel {

    @Schema(description = "用户名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "是否启用")
    private int enable;

    @Schema(description = "工号")
    private String employeeId;

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