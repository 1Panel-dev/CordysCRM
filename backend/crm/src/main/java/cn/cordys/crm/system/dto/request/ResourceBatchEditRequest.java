package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class ResourceBatchEditRequest {

  @NotEmpty
  @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> ids;

  @NotBlank
  @Schema(description = "字段ID或字段key")
  private String fieldId;

  @Schema(description = "字段值")
  private Object fieldValue;
}
