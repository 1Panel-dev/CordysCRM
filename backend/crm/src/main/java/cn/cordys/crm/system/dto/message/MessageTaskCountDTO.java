package cn.cordys.crm.system.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageTaskCountDTO {
    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "数量")
    private Integer count;

    @Schema(description = "自定义通知名称")
    private String name;
}
