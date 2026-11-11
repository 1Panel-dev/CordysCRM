package cn.cordys.crm.form.dto.request;

import cn.cordys.crm.system.dto.request.UserViewAddRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomFormUserViewAddRequest extends UserViewAddRequest {

    @Schema(description = "自定义表单ID")
    @NotBlank
    private String customFormId;
}
