package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonTypeName(value = "DATA_SOURCE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceField extends BaseField {

	@Schema(description = "是否是多选")
	private Boolean multiple;

	@Schema(description = "数据源类型", allowableValues = {"CUSTOMER", "CONTACT", "BUSINESS", "PRODUCT", "LEAD"})
	private String dataSourceType;

	@Schema(description = "默认值")
	private List<String> defaultValue = new ArrayList<>();

	@Schema(description = "初始化选项")
	private List<OptionDTO> initialOptions;
}
