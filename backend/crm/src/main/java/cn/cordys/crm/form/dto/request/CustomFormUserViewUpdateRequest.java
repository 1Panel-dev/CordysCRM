package cn.cordys.crm.form.dto.request;

import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomFormUserViewUpdateRequest extends UserViewUpdateRequest {

    @Schema(description = "自定义表单ID")
    @NotBlank
    private String customFormId;
}
