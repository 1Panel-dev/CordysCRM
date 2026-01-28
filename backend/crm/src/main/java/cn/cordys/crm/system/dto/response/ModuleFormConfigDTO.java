package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class ModuleFormConfigDTO {

  @Schema(description = "字段集合及其属性")
  private List<BaseField> fields;

  @Schema(description = "表单属性")
  private FormProp formProp;
}
