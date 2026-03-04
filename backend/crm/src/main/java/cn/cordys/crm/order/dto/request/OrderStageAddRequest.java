package cn.cordys.crm.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrderStageAddRequest {

    @Schema(description = "值")
    private String name;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "添加的位置（取值：-1，,1。 -1：源节点之前，1：源节点之后）", requiredMode = Schema.RequiredMode.REQUIRED)
    private int dropPosition;

    @Schema(description = "源节点")
    private String targetId;
}
