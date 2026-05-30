package cn.cordys.crm.system.dto.request;

import cn.cordys.crm.system.dto.FilterConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CapacityUpdateRequest {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String id;
    @NotNull
    @Schema(description = "范围ID集合")
    private List<String> scopeIds;
    @Schema(description = "容量")
    private Integer capacity;
    @Schema(description = "过滤条件集合")
    private List<FilterConditionDTO> filters;
}
