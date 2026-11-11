package cn.cordys.crm.form.dto.request;

import cn.cordys.common.dto.request.PosRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomFormPosRequest extends PosRequest {

    @Schema(description = "自定义表单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String customFormId;
}
