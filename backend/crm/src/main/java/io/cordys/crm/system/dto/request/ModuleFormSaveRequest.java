package io.cordys.crm.system.dto.request;

import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleFormSaveRequest {

	@Size(max = 20)
	@NotBlank
	@Schema(description = "表单KEY", requiredMode = Schema.RequiredMode.REQUIRED)
	private String formKey;

	@Schema(description = "保存字段集合")
	private List<ModuleFieldDTO> fields;

	@NonNull
	@Schema(description = "表单属性")
	private Map<String, Object> formProp;

	@Schema(description = "删除字段ID集合")
	private List<String> deleteFieldIds;
}