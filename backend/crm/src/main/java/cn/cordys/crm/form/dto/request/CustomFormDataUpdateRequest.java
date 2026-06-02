package cn.cordys.crm.form.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CustomFormDataUpdateRequest {

    @NotBlank
    @Schema(description = "ID")
    private String id;

    @NotBlank
    @Schema(description = "自定义表单ID")
    private String customFormId;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "名称")
    private String name;

    @Size(max = 32)
    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "模块字段值")
    private List<BaseModuleFieldValue> moduleFields;
}
