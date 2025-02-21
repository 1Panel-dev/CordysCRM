package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class BusinessModuleFieldDTO extends ModuleFieldDTO {
	@Schema(description = "业务模块字段（定义在主表中，有特定业务含义）")
	private String businessKey;
}
