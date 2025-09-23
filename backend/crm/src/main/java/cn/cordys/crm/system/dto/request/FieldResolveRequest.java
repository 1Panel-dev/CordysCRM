package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FieldResolveRequest {

    @NotBlank
    @Schema(description = "字段业务来源", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceType;
    @NotEmpty
    @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> names;
}
