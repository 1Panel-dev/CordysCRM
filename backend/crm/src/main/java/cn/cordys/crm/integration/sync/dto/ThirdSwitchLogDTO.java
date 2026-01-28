package cn.cordys.crm.integration.sync.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdSwitchLogDTO {

  @Schema(description = "应用类型")
  private String thirdType;
}
