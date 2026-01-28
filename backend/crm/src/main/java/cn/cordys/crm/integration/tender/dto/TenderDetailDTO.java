package cn.cordys.crm.integration.tender.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TenderDetailDTO {

  @Schema(description = "地址", requiredMode = Schema.RequiredMode.REQUIRED)
  private String tenderAddress;

  @Schema(description = "是否验证通过")
  private Boolean verify;
}
