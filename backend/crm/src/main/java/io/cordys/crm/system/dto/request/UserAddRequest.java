package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserAddRequest {

    @Size(max = 255)
    @Schema(description = "姓名")
    @NotBlank
    private String name;

    @Size(max = 11)
    @Schema(description = "手机号")
    @NotBlank
    private String phone;

    @Schema(description = "性别(false-男/ture-女)")
    @NotNull
    private Boolean gender;

    @Pattern(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "{email.format_error}")
    @Schema(description = "邮箱")
    @NotBlank
    private String email;

    @Schema(description = "部门id")
    @NotBlank
    private String departmentId;

    @Schema(description = "工号")
    private String employeeId;

    @Schema(description = "员工类型")
    private String employeeType;

    @Schema(description = "直属上级")
    private String supervisorId;

    @Schema(description = "工作城市")
    private String workCity;

    @Schema(description = "角色")
    private List<String> roleIds;

    @Schema(description = "用户组")
    private List<String> userGroupIds;

    @Schema(description = "是否启用")
    private Boolean enable;

}
