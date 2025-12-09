package cn.cordys.crm.system.dto.field.base;

import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author song-cc-rock
 */
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
        @JsonSubTypes.Type(value = SelectMultipleField.class, name = "SELECT_MULTIPLE"),
        @JsonSubTypes.Type(value = InputMultipleField.class, name = "INPUT_MULTIPLE"),
        @JsonSubTypes.Type(value = MemberField.class, name = "MEMBER"),
        @JsonSubTypes.Type(value = MemberMultipleField.class, name = "MEMBER_MULTIPLE"),
        @JsonSubTypes.Type(value = DepartmentField.class, name = "DEPARTMENT"),
        @JsonSubTypes.Type(value = DepartmentMultipleField.class, name = "DEPARTMENT_MULTIPLE"),
        @JsonSubTypes.Type(value = DividerField.class, name = "DIVIDER"),
        @JsonSubTypes.Type(value = PictureField.class, name = "PICTURE"),
        @JsonSubTypes.Type(value = LocationField.class, name = "LOCATION"),
        @JsonSubTypes.Type(value = PhoneField.class, name = "PHONE"),
        @JsonSubTypes.Type(value = DatasourceField.class, name = "DATA_SOURCE"),
        @JsonSubTypes.Type(value = DatasourceMultipleField.class, name = "DATA_SOURCE_MULTIPLE"),
        @JsonSubTypes.Type(value = SerialNumberField.class, name = "SERIAL_NUMBER"),
        @JsonSubTypes.Type(value = AttachmentField.class, name = "ATTACHMENT"),
        @JsonSubTypes.Type(value = LinkField.class, name = "LINK"),
        @JsonSubTypes.Type(value = IndustryField.class, name = "INDUSTRY"),
        @JsonSubTypes.Type(value = ProductSubField.class, name = "SUB_PRODUCT"),
        @JsonSubTypes.Type(value = PriceSubField.class, name = "SUB_PRICE"),
        @JsonSubTypes.Type(value = FormulaField.class, name = "FORMULA"),
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

    @Schema(description = "是否移动端")
    private Boolean mobile;

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

    @Schema(description = "校验规则详情")
    private List<RuleProp> rules = new ArrayList<>();

    @Schema(description = "显隐规则")
    private List<ControlRuleProp> showControlRules;

    @Schema(description = "业务模块字段（定义在主表中，有特定业务含义）")
    private String businessKey;

    @Schema(description = "禁止修改的参数")
    private Set<String> disabledProps;

	@Schema(description = "引用的数据源字段ID")
	private String resourceFieldId;

	@Schema(description = "引用的子表格字段ID")
	private String subTableFieldId;

    @JsonIgnore
    public static boolean isBlob(String type) {
        return Strings.CS.equalsAny(type, FieldType.TEXTAREA.name(), FieldType.INPUT_MULTIPLE.name(),
                FieldType.MEMBER_MULTIPLE.name(), FieldType.DEPARTMENT_MULTIPLE.name(), FieldType.SELECT_MULTIPLE.name(), FieldType.CHECKBOX.name(),
                FieldType.DATA_SOURCE_MULTIPLE.name(), FieldType.ATTACHMENT.name(), FieldType.LINK.name());
    }

    @JsonIgnore
    public boolean isBlob() {
        return isBlob(this.type);
    }

    @JsonIgnore
    public boolean isAttachment() {
        return Strings.CS.equalsAny(type, FieldType.PICTURE.name(), FieldType.ATTACHMENT.name());
    }

    @JsonIgnore
    public boolean isSerialNumber() {
        return Strings.CS.equals(type, FieldType.SERIAL_NUMBER.name());
    }

	@JsonIgnore
	public boolean isSubField() {
		return Strings.CS.equalsAny(type, FieldType.SUB_PRICE.name(), FieldType.SUB_PRODUCT.name());
	}

    @JsonIgnore
    public boolean needRepeatCheck() {
        return Strings.CS.equalsAny(type, FieldType.INPUT.name(), FieldType.PHONE.name())
                && rules.stream().anyMatch(rule -> Strings.CS.equals(rule.getKey(), "unique"));
    }

    @JsonIgnore
    public boolean needInitialOptions() {
        return Strings.CS.equalsAny(type, FieldType.MEMBER.name(), FieldType.DEPARTMENT.name());
    }

    @JsonIgnore
    public boolean hasSingleOptions() {
        return Strings.CS.equalsAny(type, FieldType.RADIO.name(), FieldType.CHECKBOX.name(), FieldType.SELECT.name(),
                FieldType.DATA_SOURCE.name(), FieldType.MEMBER.name(), FieldType.DEPARTMENT.name());
    }

    @JsonIgnore
    public boolean hasOptions() {
        return hasSingleOptions() || Strings.CS.equalsAny(type, FieldType.SELECT_MULTIPLE.name(),
                FieldType.MEMBER_MULTIPLE.name(), FieldType.DEPARTMENT_MULTIPLE.name(),
                FieldType.DATA_SOURCE_MULTIPLE.name());
    }

    @JsonIgnore
    public boolean canImport() {
		// 序列号、附件、图片、分割线, 计算 不支持导入.
        return !Strings.CS.equalsAny(type, FieldType.SERIAL_NUMBER.name()) && !Strings.CS.equalsAny(type, FieldType.ATTACHMENT.name())
                && !Strings.CS.equalsAny(type, FieldType.PICTURE.name()) && !Strings.CS.equalsAny(type, FieldType.DIVIDER.name())
				&& !Strings.CS.equals(type, FieldType.FORMULA.name()) && readable;
    }

	@JsonIgnore
	public boolean canDisplay() {
		// 公式、附件、图片、分割线, 子表格, 不可见字段等这些, 不支持在子列表等场景展示.
		return !Strings.CS.equalsAny(type, FieldType.FORMULA.name()) && !Strings.CS.equalsAny(type, FieldType.ATTACHMENT.name())
				&& !Strings.CS.equalsAny(type, FieldType.SUB_PRICE.name()) && !Strings.CS.equalsAny(type, FieldType.SUB_PRODUCT.name())
				&& !Strings.CS.equalsAny(type, FieldType.PICTURE.name()) && !Strings.CS.equalsAny(type, FieldType.DIVIDER.name()) && readable;
	}

    @JsonIgnore
    public boolean needRequireCheck() {
        return rules.stream().anyMatch(rule -> Strings.CS.equals(rule.getKey(), "required"));
    }

    @JsonIgnore
    public boolean multiple() {
        return Strings.CS.equalsAny(type, FieldType.CHECKBOX.name(), FieldType.SELECT_MULTIPLE.name(),
                FieldType.INPUT_MULTIPLE.name(), FieldType.MEMBER_MULTIPLE.name(), FieldType.DEPARTMENT_MULTIPLE.name(), FieldType.DATA_SOURCE_MULTIPLE.name());
    }

    @JsonIgnore
    public boolean needComment() {
        return Strings.CS.equalsAny(type, FieldType.RADIO.name(), FieldType.CHECKBOX.name(), FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name(),
                FieldType.LOCATION.name(), FieldType.INPUT_NUMBER.name(), FieldType.DATE_TIME.name(), FieldType.INDUSTRY.name());
    }

    @JsonIgnore
    public boolean isLocation() {
        return Strings.CS.equals(type, FieldType.LOCATION.name());
    }

    @JsonIgnore
    public boolean isIndustry() {
        return Strings.CS.equals(type, FieldType.INDUSTRY.name());
    }

	@JsonIgnore
	public String idOrBusinessKey() {
		return this.getBusinessKey() != null ? this.getBusinessKey() : this.getId();
	}
}
