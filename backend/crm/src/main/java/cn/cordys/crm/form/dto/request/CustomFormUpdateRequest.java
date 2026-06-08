package cn.cordys.crm.form.dto.request;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    @Schema(description = "保存字段集合")
    private List<BaseField> fields;

    @Schema(description = "表单属性")
    private FormProp formProp;
}
