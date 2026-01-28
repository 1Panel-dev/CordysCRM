package cn.cordys.crm.integration.dataease.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeAuthDTO {

  @Schema(description = "token")
  private String token;

  @Schema(description = "url")
  private String url;
}
