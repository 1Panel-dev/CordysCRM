package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianxing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleUserTreeNode extends BaseTreeNode {
    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "是否是内置角色")
    private Boolean internal = false;

    @Schema(description = "是否启用")
    private Boolean enabled = true;
}
