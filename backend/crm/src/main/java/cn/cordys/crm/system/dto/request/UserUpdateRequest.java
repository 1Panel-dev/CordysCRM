package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest extends UserAddRequest {

  @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  private String id;
}
