package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class BusinessModuleFormConfigDTO {

	@Schema(description = "字段集合及其属性")
	private List<BusinessModuleFieldDTO> fields;

	@Schema(description = "表单属性")
	private FormProp formProp;
}
