package cn.cordys.crm.system.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.system.constants.DictModule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictSwitchRequest {

	@NotEmpty
	@EnumValue(enumClass = DictModule.class)
	@Schema(description = "字典模块", requiredMode = Schema.RequiredMode.REQUIRED)
	private String module;

	@NotNull
	@Schema(description = "开启关闭", requiredMode = Schema.RequiredMode.REQUIRED)
	private Boolean enable;
}
