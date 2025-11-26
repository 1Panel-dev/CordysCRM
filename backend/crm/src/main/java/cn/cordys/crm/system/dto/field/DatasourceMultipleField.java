package cn.cordys.crm.system.dto.field;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "DATA_SOURCE_MULTIPLE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceMultipleField extends DatasourceField {

}
