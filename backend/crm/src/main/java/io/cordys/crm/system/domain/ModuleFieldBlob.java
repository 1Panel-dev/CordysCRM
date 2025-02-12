package io.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_module_field_blob")
public class ModuleFieldBlob {

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Schema(description = "额外属性")
	private byte[] extraProp;

	@Schema(description = "校验规则")
	private byte[] rules;
}
