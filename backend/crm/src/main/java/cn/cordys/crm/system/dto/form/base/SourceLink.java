package cn.cordys.crm.system.dto.form.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author song-cc-rock
 */
@Data
public class SourceLink {

	@NotEmpty
	@Schema(description = "当前字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String current;

	@NotEmpty
	@Schema(description = "联动字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String link;

	@NotEmpty
	@Schema(description = "联动方式", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "fill", allowableValues = {"fill"})
	private String method;

	@Schema(description = "是否启用联动配置", defaultValue = "true")
	private boolean enable;

	@Schema(description = "子表格联动配置(仅为子表格时需要配置)")
	private List<SourceLink> childLinks;
}
