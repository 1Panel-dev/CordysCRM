package cn.cordys.crm.integration.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdConfigBaseDTO<T> {

  @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED)
  private String type;

  @Schema(description = "是否验证通过", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean verify;

  @Schema(description = "配置属性对象", requiredMode = Schema.RequiredMode.REQUIRED)
  private T config;
}
