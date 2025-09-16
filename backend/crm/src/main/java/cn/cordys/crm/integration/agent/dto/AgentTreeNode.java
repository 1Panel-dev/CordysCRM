package cn.cordys.crm.integration.agent.dto;

import cn.cordys.common.dto.BaseTreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentTreeNode extends BaseTreeNode {

    @Schema(description = "节点类型, 如:智能体文件夹, 智能体")
    private String type;

    @Schema(description = "是否收藏")
    private boolean myCollect = false;

}
