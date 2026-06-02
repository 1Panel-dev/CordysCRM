package cn.cordys.crm.form.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CustomFormRoleUserBatchRequest {

    @NotBlank
    @Schema(description = "角色ID")
    private String roleId;

    @NotEmpty
    @Schema(description = "用户ID列表")
    private List<String> userIds;
}
