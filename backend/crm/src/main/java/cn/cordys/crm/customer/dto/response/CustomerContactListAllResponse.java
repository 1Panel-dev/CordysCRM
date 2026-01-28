package cn.cordys.crm.customer.dto.response;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
public class CustomerContactListAllResponse {
  @Schema(description = "列表数据")
  private List<CustomerContactListResponse> list;

  /** 选项集合 */
  @Schema(description = "选项集合")
  private Map<String, List<OptionDTO>> optionMap;
}
