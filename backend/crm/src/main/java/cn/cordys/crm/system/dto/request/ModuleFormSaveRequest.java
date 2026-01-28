package cn.cordys.crm.system.dto.request;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleFormSaveRequest {

  @Size(max = 50)
  @NotBlank
  @Schema(description = "表单KEY", requiredMode = Schema.RequiredMode.REQUIRED)
  private String formKey;

  @Schema(description = "保存字段集合")
  private List<BaseField> fields;

  @NotNull
  @Schema(description = "表单属性")
  private FormProp formProp;
}
