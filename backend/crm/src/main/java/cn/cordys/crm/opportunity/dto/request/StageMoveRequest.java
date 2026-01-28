package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class StageMoveRequest {

  @Schema(description = "ids")
  private List<String> ids;
}
