package io.cordys.crm.system.dto.response;

import io.cordys.crm.customer.dto.response.CustomerRepeatResponse;
import io.cordys.crm.lead.dto.LeadRepeatDTO;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class RepeatCustomerResponse {

    @Schema(description = "客户重复数据")
    private CustomerRepeatResponse customerData;

    @Schema(description = "线索重复数据")
    private List<LeadRepeatDTO>leadList;

    @Schema(description = "商机重复数据")
    private List<OpportunityListResponse>opportunityList;

}
