package cn.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "DATA_SOURCE_MULTIPLE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceMultipleField extends DatasourceField {

}
