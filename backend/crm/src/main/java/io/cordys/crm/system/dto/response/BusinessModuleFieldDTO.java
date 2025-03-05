package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * @author jianxing
 */
@Data
public class BusinessModuleFieldDTO extends BaseField {
	@Schema(description = "业务模块字段（定义在主表中，有特定业务含义）")
	private String businessKey;

	@Schema(description = "禁止修改的参数")
	private Set<String> disabledProps;
}
