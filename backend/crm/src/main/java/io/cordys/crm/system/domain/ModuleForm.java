package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_module_form")
public class ModuleForm extends BaseModel {

	@Schema(description = "表单Key")
	private String formKey;

	@Schema(description = "组织id")
	private String organizationId;
}
