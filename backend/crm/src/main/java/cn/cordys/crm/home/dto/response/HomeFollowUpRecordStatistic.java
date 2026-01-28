package cn.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: jianxing @CreateTime: 2025-07-18 16:07
 */
@Data
public class HomeFollowUpRecordStatistic {

  @Schema(description = "新增跟进记录")
  private HomeStatisticSearchResponse newFollowUpRecord;
}
