package io.cordys.crm.customer.dto.response;

import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityRepeatResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomerRepeatResponse {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "负责人部门")
    private String ownerDepartmentId;

    @Schema(description = "负责人部门名称")
    private String ownerDepartmentName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "线索重复数据")
    private List<ClueListResponse> clueList;

    @Schema(description = "商机重复数据")
    private List<OpportunityRepeatResponse>opportunityList;

}
