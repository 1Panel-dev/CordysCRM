package cn.cordys.crm.dashboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DashboardEditPosRequest {

    @Schema(description = "文件夹id")
    private String dashboardModuleId;

    @Schema(description = "移动仪表板id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{source_id.not_blank}")
    private String moveId;

    @Schema(description = "目标仪表板id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{target_id.not_blank}")
    private String targetId;

    @Schema(description = "移动类型", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"BEFORE", "AFTER", "APPEND"})
    @NotBlank(message = "{moveMode.not_blank}")
    private String moveMode;


}
