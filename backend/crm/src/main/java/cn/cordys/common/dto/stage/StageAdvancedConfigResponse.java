package cn.cordys.common.dto.stage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StageAdvancedConfigResponse extends CirculationSetting {

    @Schema(description = "ID")
    private String id;
}
