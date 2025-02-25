package io.cordys.crm.system.dto.field.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cordys.crm.system.dto.field.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
		@JsonSubTypes.Type(value = InputFieldProp.class, name = "INPUT"),
		@JsonSubTypes.Type(value = TextAreaFieldProp.class, name = "TEXTAREA"),
		@JsonSubTypes.Type(value = NumberFieldProp.class, name = "NUMBER"),
		@JsonSubTypes.Type(value = DateTimeFieldProp.class, name = "DATETIME"),
		@JsonSubTypes.Type(value = RadioFieldProp.class, name = "RADIO"),
		@JsonSubTypes.Type(value = CheckBoxFieldProp.class, name = "CHECKBOX"),
		@JsonSubTypes.Type(value = SelectFieldProp.class, name = "SELECT"),
		@JsonSubTypes.Type(value = TagFieldProp.class, name = "TAG"),
		@JsonSubTypes.Type(value = LocationFieldProp.class, name = "LOCATION"),
		@JsonSubTypes.Type(value = MemberFieldProp.class, name = "MEMBER"),
		@JsonSubTypes.Type(value = DividerFieldProp.class, name = "DIVIDER"),
})
public abstract class BaseFieldProp {

	@Schema(description = "名称")
	private String name;
	@Schema(description = "内置Key")
	private String key;
	@Schema(description = "类型")
	private String type;
	@Schema(description = "是否展示标题")
	private Boolean showLabel;
	@Schema(description = "占位文本")
	private String placeholder;
	@Schema(description = "描述")
	private String description;
	@Schema(description = "默认值")
	private String defaultValue;
	@Schema(description = "是否可读")
	private Boolean readable;
	@Schema(description = "是否可编辑")
	private Boolean editable;
	@Schema(description = "字段宽度")
	private String fieldWidth;
	@Schema(description = "校验规则")
	private List<BaseRuleProp> rules;
}
