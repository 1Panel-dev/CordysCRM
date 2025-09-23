package cn.cordys.crm.system.dto.field;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@JsonTypeName(value = "DATA_SOURCE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceField extends BaseField {
    @EnumValue(enumClass = FieldSourceType.class)
    @Schema(description = "数据源类型")
    private String dataSourceType;
    @Schema(description = "过滤条件")
    private Map<String, Object> combineSearch;
}
