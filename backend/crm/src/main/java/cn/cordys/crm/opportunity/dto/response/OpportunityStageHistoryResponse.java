package cn.cordys.crm.opportunity.dto.response;

import cn.cordys.crm.opportunity.domain.OpportunityStageHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class OpportunityStageHistoryResponse extends OpportunityStageHistory {
    @Schema(description = "原阶段名称")
    private String fromStageName;

    @Schema(description = "新阶段名称")
    private String toStageName;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "失败原因名称")
    private String failureReasonName;
}
