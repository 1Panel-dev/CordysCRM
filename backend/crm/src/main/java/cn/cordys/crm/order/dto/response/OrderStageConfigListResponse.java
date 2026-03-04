package cn.cordys.crm.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OrderStageConfigListResponse {

    @Schema(description = "订单状态流配置列表")
    List<OrderStageConfigResponse> stageConfigList;

    @Schema(description = "进行中回退设置")
    private Boolean afootRollBack = true;

    @Schema(description = "完结回退设置")
    private Boolean endRollBack = false;


}
