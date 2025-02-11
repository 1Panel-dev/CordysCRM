package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.domain.ModuleFieldOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ModuleFieldDTO {

	@Schema(description = "ID")
	private String id;

	@Size(max = 32)
	@NotBlank
	@Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String moduleId;

	@Size(max = 255)
	@NotBlank
	@Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@Schema(description = "字段内置Key")
	private String internalKey;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "字段类型", allowableValues = {"input", "textarea", "number", "datetime", "radio", "checkbox", "select",
			"multi_select", "member", "multi_member", "department", "multi_department", "divider"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String type;

	@NotNull
	@Schema(description = "显示标题", defaultValue = "true")
	private Boolean showLabel;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "提示文字")
	private String tooltip;

	@Schema(description = "默认值 (支持多个值)")
	private String defaultValue;

	@NotNull
	@Schema(description = "可见", defaultValue = "true")
	private Boolean readable;

	@NotNull
	@Schema(description = "可编辑", defaultValue = "true")
	private Boolean editable;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "自定义宽度", allowableValues = {"full"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String fieldWidth;

	@NotNull
	@Schema(description = "排序")
	private Long pos;

	@Schema(description = "额外属性(JSON)")
	private String extraProp;

	@Schema(description = "校验规则(JSON)")
	private String rules;

	/**
	 * TODO: 参数需替换
	 */
	@Schema(description = "字段选项值")
	private List<ModuleFieldOption> options;
}
