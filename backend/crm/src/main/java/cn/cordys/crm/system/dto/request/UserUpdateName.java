package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateName {

    @Schema(description = "用户id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String userId;

    @Size(max = 255)
    @Schema(description = "姓名")
    @NotBlank
    private String name;
}
