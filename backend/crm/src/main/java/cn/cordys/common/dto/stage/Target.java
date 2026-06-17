package cn.cordys.common.dto.stage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class Target {

    @Schema(description = "目标id")
    private String targetId;

    @Schema(description = "是否允许流转")
    private Boolean enable;

    @Schema(description = "字段配置")
    private List<CirculationFieldValue> circulationFieldValues;
}
