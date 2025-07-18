package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.common.constants.EnumValue;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.crm.system.constants.FieldSourceType;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@JsonTypeName(value = "DATA_SOURCE_MULTIPLE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceMultipleField extends BaseField {
	@EnumValue(enumClass = FieldSourceType.class)
	@Schema(description = "数据源类型")
	private String dataSourceType;
	@Schema(description = "过滤条件")
	private Map<String, Object> combineSearch;
}
