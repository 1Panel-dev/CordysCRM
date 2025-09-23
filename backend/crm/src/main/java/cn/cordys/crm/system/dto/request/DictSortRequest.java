package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictSortRequest {

    @NotEmpty
    @Schema(description = "字典ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dragDictId;

    @NotNull
    @Schema(description = "排序前", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long start;

    @NotNull
    @Schema(description = "排序后", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long end;
}
