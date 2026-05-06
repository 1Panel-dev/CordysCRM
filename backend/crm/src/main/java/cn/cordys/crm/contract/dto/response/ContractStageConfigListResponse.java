package cn.cordys.crm.contract.dto.response;

import cn.cordys.common.dto.stage.StageConfigResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ContractStageConfigListResponse {

    @Schema(description = "订单状态流配置列表")
    List<StageConfigResponse> stageConfigList;

    @Schema(description = "进行中回退设置")
    private Boolean afootRollBack = true;

    @Schema(description = "完结回退设置")
    private Boolean endRollBack = false;
}
