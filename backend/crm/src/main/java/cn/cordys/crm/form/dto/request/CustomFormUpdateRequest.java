package cn.cordys.crm.form.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomFormUpdateRequest {

    @NotBlank
    @Schema(description = "ID")
    private String id;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "名称")
    private String name;

    @Schema(description = "是否启用")
    private Boolean enable;
}
