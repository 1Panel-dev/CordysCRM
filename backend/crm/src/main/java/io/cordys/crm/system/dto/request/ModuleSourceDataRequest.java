package io.cordys.crm.system.dto.request;

import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleSourceDataRequest extends BasePageRequest {

	@Schema(description = "表单Key")
	private String formKey;
	@Schema(description = "字段ID")
	private String fieldId;
	@Schema(description = "数据来源类型", allowableValues = {"CUSTOMER", "CONTACT", "BUSINESS", "PRODUCT"})
	private String sourceType;
}
