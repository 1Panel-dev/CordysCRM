package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseFieldProp;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "INPUT")
@EqualsAndHashCode(callSuper = true)
public class InputFieldProp extends BaseFieldProp {


}
