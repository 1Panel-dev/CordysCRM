package cn.cordys.crm.system.dto;

import cn.cordys.crm.system.domain.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DictConfigDTO {

  @Schema(description = "字典值是否启用")
  private Boolean enable;

  @Schema(description = "字典值列表")
  private List<Dict> dictList;
}
