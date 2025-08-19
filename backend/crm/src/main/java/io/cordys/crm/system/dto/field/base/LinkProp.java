package io.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LinkProp implements Serializable {

	@NotEmpty
	@Schema(description = "联动字段", requiredMode = Schema.RequiredMode.REQUIRED)
	private String targetField;
	@Schema(description = "联动选项配置")
	private List<LinkOption> linkOptions;

	@Data
	static class LinkOption {

		@NotBlank
		@Schema(description = "当前字段选项", requiredMode = Schema.RequiredMode.REQUIRED)
		private List<String> current;

		@NotEmpty
		@Schema(description = "联动方式", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"AUTO", "HIDDEN"})
		private String method;

		@NotBlank
		@Schema(description = "联动字段选项", requiredMode = Schema.RequiredMode.REQUIRED)
		private List<String> target;
	}
}
