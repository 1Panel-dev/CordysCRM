package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "INPUT_MULTIPLE")
@EqualsAndHashCode(callSuper = true)
public class InputMultipleField extends BaseField {}
