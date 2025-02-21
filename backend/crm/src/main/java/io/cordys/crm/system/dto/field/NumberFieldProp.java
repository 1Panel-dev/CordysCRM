package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseFieldProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "NUMBER")
@EqualsAndHashCode(callSuper = true)
public class NumberFieldProp extends BaseFieldProp {

	@Schema(description = "格式")
	private String format;
}
