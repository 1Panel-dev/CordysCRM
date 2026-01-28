package cn.cordys.crm.search.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class FieldMaskConfigDTO {

  @Schema(description = "搜索字段集合")
  private Map<String, List<String>> searchFields;
}
