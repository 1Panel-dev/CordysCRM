package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessTitleImportRequest {

    @NotBlank
    @Schema(description = "导入类型")
    private String importType;
}
