package cn.cordys.crm.system.dto.response;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OptionScopeDTO extends OptionDTO {

  @Schema(description = "类型")
  private String scope;
}
