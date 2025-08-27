package cn.cordys.crm.dashboard.dto.request;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DashboardAddRequest {

    @Size(min = 1, max = 255, message = "{department.name.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{dashboard.name.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "仪表板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "仪表板id")
    @NotBlank(message = "{dashboard.parent.id.not_blank}")
    private String resourceId;

    @Schema(description = "文件夹id")
    @NotBlank(message = "{dashboard.parent.id.not_blank}")
    private String dashboardModuleId;

    @NotNull
    @Schema(description = "范围ID集合")
    private List<String> scopeIds;

    @Schema(description = "描述")
    private String description;

}
