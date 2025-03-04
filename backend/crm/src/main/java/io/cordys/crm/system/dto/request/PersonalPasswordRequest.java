package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonalPasswordRequest {

    @Pattern(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "{email.format_error}")
    @Schema(description = "邮箱")
    @NotBlank
    private String email;

    @Size(max = 6)
    @Schema(description = "验证码")
    @NotBlank
    private String code;

    @Schema(description = "新密码")
    @NotBlank
    private String password;

}
