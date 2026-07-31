package cn.cordys.crm.form.dto.request;

import cn.cordys.common.dto.ExportSelectRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomFormExportSelectRequest extends ExportSelectRequest {
    @NotBlank
    @Schema(description = "自定义表单ID")
    private String customFormId;
}
