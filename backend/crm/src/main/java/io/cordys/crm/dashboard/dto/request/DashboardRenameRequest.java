package io.cordys.crm.dashboard.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DashboardRenameRequest {

    @NotBlank(message = "{dashboard.id.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Size(min = 1, max = 255, message = "{department.name.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{dashboard.name.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{dashboardModule.id.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "当前文件夹id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dashboardModuleId;
}
