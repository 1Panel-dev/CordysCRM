package io.cordys.crm.system.dto.request;


import io.cordys.common.dto.condition.CombineSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserViewAddRequest extends CombineSearch {

    @Schema(description = "视图名称")
    @NotBlank
    private String name;

    @Schema(description = "视图类型(客户/线索/商机等)")
    @NotBlank
    private String resourceType;
}
