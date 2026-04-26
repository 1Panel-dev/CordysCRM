package cn.cordys.crm.opportunity.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "opportunity_stage_history")
public class OpportunityStageHistory {

    @Schema(description = "id")
    private String id;

    @Schema(description = "商机id")
    private String opportunityId;

    @Schema(description = "原阶段id")
    private String fromStage;

    @Schema(description = "新阶段id")
    private String toStage;

    @Schema(description = "变更时间")
    private Long changeTime;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "失败原因id（当变更为失败阶段时）")
    private String failureReasonId;
}
