package cn.cordys.crm.system.dto.request;


import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.system.constants.DictModule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DictAddRequest {

	@NotEmpty
	@Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@NotEmpty
	@Schema(description = "字典模块", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"OPPORTUNITY_FAIL_RS", "CUSTOMER_POOL_RS", "CLUE_POOL_RS"})
	@EnumValue(enumClass = DictModule.class)
	private String module;
}
