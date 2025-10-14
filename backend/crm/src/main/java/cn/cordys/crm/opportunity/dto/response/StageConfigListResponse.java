package cn.cordys.crm.opportunity.dto.response;

import cn.cordys.crm.opportunity.domain.OpportunityStageConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class StageConfigListResponse {

    @Schema(description = "商机阶段配置列表")
    List<OpportunityStageConfig> stageConfigList;

    @Schema(description = "进行中回退设置")
    private Boolean afootRollBack = true;

    @Schema(description = "完结回退设置")
    private Boolean endRollBack = false;

    @Schema(description = "当前阶段是否存在数据")
    private Boolean stageHasData = false;
}
