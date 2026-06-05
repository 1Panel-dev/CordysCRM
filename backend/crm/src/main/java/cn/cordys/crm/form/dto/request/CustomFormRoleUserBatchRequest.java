package cn.cordys.crm.form.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CustomFormRoleUserBatchRequest {

    @NotBlank
    @Schema(description = "表单角色ID")
    private String customFormRoleId;

    @Schema(description = "用户ID列表")
    private List<String> userIds;

    @Schema(description = "部门ID列表")
    private List<String> deptIds;

    @Schema(description = "系统角色ID列表")
    private List<String> roleIds;
}
