package cn.cordys.common.dto.stage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CirculationSetting {

    @Schema(description = "源id")
    private String originId;

    @Schema(description = "源id对应的行目标ids")
    private List<Target> targets;

    @Schema(description = "模块类型(order-订单/contract-合同)")
    private String moduleType;
}
