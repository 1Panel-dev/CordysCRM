package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.field.base.OptionProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@JsonTypeName(value = "RADIO")
@EqualsAndHashCode(callSuper = true)
public class RadioField extends BaseField {

	@Schema(description = "选项值")
	private List<OptionProp> options;

	@Schema(description = "分布方式", allowableValues = {"horizontal", "vertical"})
	private String direction;

}
