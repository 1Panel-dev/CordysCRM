package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeSortDTO {

    @Schema(description = "节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{file_module.not.exist}")
    private BaseTree node;

    @Schema(description = "父节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{file_module.parent.not.exist}")
    private BaseTree parent;

    @Schema(description = "前一个节点")
    private BaseTree previousNode;

    @Schema(description = "后一个节点")
    private BaseTree nextNode;
}

