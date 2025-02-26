package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "PHONE")
@EqualsAndHashCode(callSuper = true)
public class PhoneField extends BaseField {


}
