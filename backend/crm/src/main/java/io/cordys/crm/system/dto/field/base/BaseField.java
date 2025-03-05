package io.cordys.crm.system.dto.field.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
		@JsonSubTypes.Type(value = InputField.class, name = "INPUT"),
		@JsonSubTypes.Type(value = TextAreaField.class, name = "TEXTAREA"),
		@JsonSubTypes.Type(value = InputNumberField.class, name = "INPUT_NUMBER"),
		@JsonSubTypes.Type(value = DateTimeField.class, name = "DATE_TIME"),
		@JsonSubTypes.Type(value = RadioField.class, name = "RADIO"),
		@JsonSubTypes.Type(value = CheckBoxField.class, name = "CHECKBOX"),
		@JsonSubTypes.Type(value = SelectField.class, name = "SELECT"),
		@JsonSubTypes.Type(value = MultipleInputField.class, name = "MULTIPLE_INPUT"),
		@JsonSubTypes.Type(value = MemberField.class, name = "MEMBER"),
		@JsonSubTypes.Type(value = DepartmentField.class, name = "DEPARTMENT"),
		@JsonSubTypes.Type(value = DividerField.class, name = "DIVIDER"),
		@JsonSubTypes.Type(value = PictureField.class, name = "PICTURE"),
		@JsonSubTypes.Type(value = LocationField.class, name = "LOCATION"),
		@JsonSubTypes.Type(value = PhoneField.class, name = "PHONE"),
		@JsonSubTypes.Type(value = DatasourceField.class, name = "DATA_SOURCE"),
})
public abstract class BaseField {

	@Schema(description = "ID")
	private String id;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "字段内置Key")
	private String internalKey;

	@Schema(description = "排序")
	private Long pos;

	@Schema(description = "类型")
	private String type;

	@Schema(description = "是否展示标题")
	private Boolean showLabel;

	@Schema(description = "占位文本")
	private String placeholder;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "是否可读")
	private Boolean readable;

	@Schema(description = "是否可编辑")
	private Boolean editable;

	@Schema(description = "字段宽度")
	private Float fieldWidth;

	@Schema(description = "规则详情")
	private List<String> showRules = new ArrayList<>();

	@Schema(description = "规则详情")
	private List<RuleProp> rules = new ArrayList<>();

	@Schema(description = "业务模块字段（定义在主表中，有特定业务含义）")
	private String businessKey;

	@Schema(description = "禁止修改的参数")
	private Set<String> disabledProps;

	@JsonIgnore
	public boolean isTextArea() {
		return StringUtils.equals(type, FieldType.TEXTAREA.name());
	}
}
