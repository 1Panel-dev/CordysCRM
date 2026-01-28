package cn.cordys.crm.follow.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class FollowUpPlanDetailResponse extends FollowUpPlanListResponse {

  @Schema(description = "选项集合")
  private Map<String, List<OptionDTO>> optionMap;

  /** 附件集合 */
  @Schema(description = "附件集合")
  private Map<String, List<Attachment>> attachmentMap;
}
