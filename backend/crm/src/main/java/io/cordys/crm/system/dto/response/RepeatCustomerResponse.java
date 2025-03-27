package io.cordys.crm.system.dto.response;

import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.customer.dto.response.CustomerRepeatResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class RepeatCustomerResponse {

    @Schema(description = "客户重复数据")
    private List<CustomerRepeatResponse> customerData;

    @Schema(description = "线索重复数据")
    private List<ClueListResponse>clueList;



}
