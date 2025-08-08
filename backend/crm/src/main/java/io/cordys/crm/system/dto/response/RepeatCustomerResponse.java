package io.cordys.crm.system.dto.response;

import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.search.response.GlobalCustomerResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class RepeatCustomerResponse {

    @Schema(description = "客户重复数据")
    private List<GlobalCustomerResponse> customerData;

    @Schema(description = "线索重复数据")
    private List<ClueListResponse>clueList;



}
