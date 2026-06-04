package cn.cordys.crm.form.dto.request;

import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomFormDataBatchUpdateRequest extends ResourceBatchEditRequest {
    @NotBlank
    @Schema(description = "自定义表单ID")
    private String customFormId;
}
