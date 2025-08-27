package cn.cordys.crm.system.dto.form.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 联动配置
 */
@Data
public class LinkProp implements Serializable {

	@NotEmpty
	@Schema(description = "联动表单Key", requiredMode = Schema.RequiredMode.REQUIRED)
	private String formKey;
	@Schema(description = "联动字段集合")
	private List<LinkField> linkFields;

	@Data
	public static class LinkField {

		@NotEmpty
		@Schema(description = "当前字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
		private String current;
		@NotEmpty
		@Schema(description = "联动字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
		private String link;
	}
}
