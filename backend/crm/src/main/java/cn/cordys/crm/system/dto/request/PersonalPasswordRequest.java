package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonalPasswordRequest {
  @Schema(description = "新密码")
  @NotBlank
  private String password;

  @Schema(description = "原密码")
  @NotBlank
  private String originPassword;
}
