package cn.cordys.crm.dashboard.dto.response;

import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
public class DashboardDetailResponse {

    @Schema(description = "id")
    private String id;

    @Schema(description = "仪表板名称")
    private String name;

    @Schema(description = "仪表板id")
    private String resourceId;

    @Schema(description = "文件夹id")
    private String dashboardModuleId;

    @Schema(description = "文件夹名称")
    private String dashboardModuleName;

    @Schema(description = "可查看人员id")
    private String scopeId;

    @Schema(description = "可查看人员集合")
    private List<ScopeNameDTO> members;

    @Schema(description = "描述")
    private String description;
}
