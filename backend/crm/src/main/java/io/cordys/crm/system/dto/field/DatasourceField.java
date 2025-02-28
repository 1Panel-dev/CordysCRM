package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "DATA_SOURCE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceField extends BaseField {

	@Schema(description = "数据源类型", allowableValues = {"CUSTOMER", "CONTACT", "BUSINESS", "PRODUCT"})
	private String dataSourceType;
}
