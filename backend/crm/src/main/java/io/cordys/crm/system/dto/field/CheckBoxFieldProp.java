package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseFieldProp;
import io.cordys.crm.system.dto.field.base.OptionProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@JsonTypeName(value = "CHECKBOX")
@EqualsAndHashCode(callSuper = true)
public class CheckBoxFieldProp extends BaseFieldProp {

	@Schema(description = "选项值")
	private List<OptionProp> options;

	@Schema(description = "分布方式")
	private String position;
}
