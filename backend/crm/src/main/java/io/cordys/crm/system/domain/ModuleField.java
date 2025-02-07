package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模块字段配置;
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_module_field")
public class ModuleField extends BaseModel {

	@Schema(description = "所属模块")
	private String moduleId;

	@Schema(description = "字段名称")
	private String name;

	@Schema(description = "字段类型", allowableValues = {"input", "textarea", "number", "datetime", "radio", "checkbox", "select",
			"multi_select", "member", "multi_member", "department", "multi_department", "divider"})
	private String type;

	@Schema(description = "显示标题", defaultValue = "1")
	private int showLabel;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "提示文字")
	private String tooltip;

	@Schema(description = "默认值 (支持多个值)")
	private String defaultValue;

	@Schema(description = "个性化配置")
	private String customConfig;

	@Schema(description = "分布方式", allowableValues = {"horizontal", "vertical"})
	private String layout;

	@Schema(description = "必填", defaultValue = "0")
	private int required;

	@Schema(description = "唯一", defaultValue = "0")
	private int unique;

	@Schema(description = "可见", defaultValue = "1")
	private int readable;

	@Schema(description = "可编辑", defaultValue = "1")
	private int editable;

	@Schema(description = "自定义宽度", allowableValues = {"full"})
	private String fieldWidth;

	@Schema(description = "排序")
	private Long pos;
}