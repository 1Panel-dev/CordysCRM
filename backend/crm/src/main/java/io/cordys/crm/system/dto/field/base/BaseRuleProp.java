package io.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 校验规则属性
 */
@Data
public class BaseRuleProp {

	@Schema(description = "规则类型")
	private String type;

	@Schema(description = "是否勾选")
	private Boolean enable;
}
