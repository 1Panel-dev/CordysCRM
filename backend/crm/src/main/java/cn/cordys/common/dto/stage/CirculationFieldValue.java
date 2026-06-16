package cn.cordys.common.dto.stage;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CirculationFieldValue extends BaseModuleFieldValue {

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "默认值类型")
    private String valueType;
}
