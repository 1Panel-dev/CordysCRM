package cn.cordys.crm.dashboard.dto.response;

import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DashboardPageResponse {

    @Schema(description = "id")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "三方资源id")
    private String resourceId;

    @Schema(description = "文件夹id")
    private String dashboardModuleId;

    @Schema(description = "文件夹名称")
    private String dashboardModuleName;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "同一节点下顺序")
    private Long pos;

    @Schema(description = "范围")
    private String scopeId;

    @Schema(description = "可查看人员集合")
    private List<ScopeNameDTO> members;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "修改人名称")
    private String updateUserName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "是否收藏")
    private boolean myCollect = false;
}
