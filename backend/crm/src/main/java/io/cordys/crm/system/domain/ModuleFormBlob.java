package io.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_module_form_blob")
public class ModuleFormBlob {

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Schema(description = "属性")
	private String prop;
}
