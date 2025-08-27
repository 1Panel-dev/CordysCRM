package cn.cordys.crm.dashboard.dto;

import cn.cordys.common.dto.BaseTreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTreeNode extends BaseTreeNode {

    @Schema(description = "节点类型, 如:仪表板文件夹, 仪表板")
    private String type;

    @Schema(description = "是否收藏")
    private boolean myCollect = false;

    @Schema(description = "仪表板id")
    private String resourceId;
}
