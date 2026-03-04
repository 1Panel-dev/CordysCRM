package cn.cordys.crm.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrderStageUpdateRequest {

    @Schema(description = "id")
    private String id;

    @Schema(description = "订单状态")
    private String name;

}

