package cn.cordys.crm.system.dto.response;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SyncUserScheduleConfigResponse {

    @Schema(description = "启用/禁用")
    private boolean enable;

    @Schema(description = "同步周期")
    @NotBlank
    private String syncCycle;

    @Schema(description = "同步范围")
    private List<OptionDTO> syncScope = new ArrayList<>();

    @Schema(description = "资源类型：当前第三方平台类型")
    private String resourceType;

    @Schema(description = "定时任务下一次执行时间")
    private Long nextTriggerTime;
}
