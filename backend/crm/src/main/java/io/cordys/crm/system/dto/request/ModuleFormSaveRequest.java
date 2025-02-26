package io.cordys.crm.system.dto.request;

import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleFormSaveRequest {

	@Size(max = 20)
	@NotBlank
	@Schema(description = "表单KEY", requiredMode = Schema.RequiredMode.REQUIRED)
	private String formKey;

	@Schema(description = "保存字段集合")
	private List<BaseField> fields;

	@NotNull
	@Schema(description = "表单属性")
	private FormProp formProp;

	@Schema(description = "删除字段ID集合")
	private List<String> deleteFieldIds;
}