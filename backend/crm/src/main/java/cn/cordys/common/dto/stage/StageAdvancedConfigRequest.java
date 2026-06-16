package cn.cordys.common.dto.stage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class StageAdvancedConfigRequest {

    @Schema(description = "流转配置类型")
    private String circulationType;

    @Schema(description = "高级流转设置")
    private List<CirculationSetting> circulationSettings;
}
