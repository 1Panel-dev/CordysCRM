package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.search.response.advanced.AdvancedCustomerResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class RepeatCustomerResponse {

  @Schema(description = "客户重复数据")
  private List<AdvancedCustomerResponse> customerData;

  @Schema(description = "线索重复数据")
  private List<ClueListResponse> clueList;
}
