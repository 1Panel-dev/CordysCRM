package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseFieldProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "DIVIDER")
@EqualsAndHashCode(callSuper = true)
public class DividerFieldProp extends BaseFieldProp {

	@Schema(description = "分隔线颜色")
	private String splitColor;
	@Schema(description = "标题颜色")
	private String labelColor;
}
