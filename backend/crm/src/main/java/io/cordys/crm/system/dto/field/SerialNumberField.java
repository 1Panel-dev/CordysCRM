package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@JsonTypeName(value = "SERIAL_NUMBER")
@EqualsAndHashCode(callSuper = true)
public class SerialNumberField extends BaseField {

	@Schema(description = "流水号规则")
	private List<String> serialNumberRules;
}
