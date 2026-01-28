package cn.cordys.crm.system.dto.response;

import cn.cordys.excel.domain.ExcelErrData;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author wx
 */
@Data
public class UserImportResponse implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "成功数量")
  private int successCount;

  @Schema(description = "失败数量")
  private int failCount;

  @Schema(description = "报错信息")
  private List<ExcelErrData> errorMessages;
}
