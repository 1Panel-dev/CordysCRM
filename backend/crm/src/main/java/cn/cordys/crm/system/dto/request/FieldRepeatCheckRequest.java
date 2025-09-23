package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字段重复校验参数
 *
 * @author song-cc-rock
 */
@Data
public class FieldRepeatCheckRequest {

    @NotBlank
    @Schema(description = "字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotBlank
    @Schema(description = "值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

    @NotBlank
    @Schema(description = "表单Key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String formKey;
}
