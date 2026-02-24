package cn.cordys.crm.system.dto.field;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.base.SourceLink;
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
@JsonTypeName(value = "DATA_SOURCE")
@EqualsAndHashCode(callSuper = true)
public class DatasourceField extends BaseField {

    @EnumValue(enumClass = FieldSourceType.class)
    @Schema(description = "数据源类型")
    private String dataSourceType;

    @Schema(description = "过滤条件")
    private Map<String, Object> combineSearch;

	@Schema(description = "显示字段")
	private List<String> showFields;

	@Schema(description = "引用的字段集合")
	private List<BaseField> refFields;

	@Schema(description = "联动配置(外层普通字段)")
	private List<SourceLink> linkFields;

	@Schema(description = "子表格联动配置")
	private List<SourceLink> childLinkFields;
}
