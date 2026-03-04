package cn.cordys.crm.order.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sales_order_stage_config")
public class OrderStageConfig extends BaseModel {

    @Schema(description = "订单状态")
    private String name;

    @Schema(description = "订单类型")
    private String type;

    @Schema(description = "进行中回退设置")
    private Boolean afootRollBack;

    @Schema(description = "完结回退设置")
    private Boolean endRollBack;

    @Schema(description = "顺序")
    private Long pos;

    @Schema(description = "组织id")
    private String organizationId;
}
