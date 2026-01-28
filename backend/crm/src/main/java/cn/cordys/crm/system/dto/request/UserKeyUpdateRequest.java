package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserKeyUpdateRequest {

  @Schema(description = "user_key ID", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  private String id;

  @Schema(description = "是否永久有效", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean forever;

  @Schema(description = "到期时间")
  private Long expireTime;

  @Schema(description = "描述")
  private String description;
}
