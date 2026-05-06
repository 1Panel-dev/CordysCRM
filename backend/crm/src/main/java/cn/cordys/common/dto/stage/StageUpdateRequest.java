package cn.cordys.common.dto.stage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StageUpdateRequest {

    @Schema(description = "id")
    private String id;

    @Schema(description = "状态名称")
    private String name;

}

