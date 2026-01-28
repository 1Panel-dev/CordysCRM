package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchAffectSkipResponse {

  @Schema(description = "成功条数")
  private Integer success;

  @Schema(description = "失败条数")
  private Integer fail;

  @Schema(description = "跳过数量")
  private Integer skip;
}
