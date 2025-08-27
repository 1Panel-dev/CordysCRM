package cn.cordys.crm.dashboard.dto.request;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DashboardModuleRenameRequest {

    @NotBlank(message = "{dashboardModule.id.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Size(min = 1, max = 255, message = "{department.name.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{dashboardModule.name.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
