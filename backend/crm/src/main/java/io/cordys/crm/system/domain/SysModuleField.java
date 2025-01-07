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
public class SysModuleField extends BaseModel {

	@Schema(description = "所属模块ID")
	private String moduleId;

	@Schema(description = "字段名称")
	private String name;

	@Schema(description = "字段类型")
	private String type;

	@Schema(description = "字段排序")
	private Long pos;

	@Schema(description = "提示信息")
	private String tooltip;

	@Schema(description = "是否必填", defaultValue = "必填")
	private int required;

	@Schema(description = "是否唯一", defaultValue = "不唯一")
	private int unique;

	@Schema(description = "字段宽度")
	private String fieldWidth;

	@Schema(description = "默认值 (支持多个值)")
	private String defaultValue;

	@Schema(description = "是否系统默认属性", defaultValue = "是")
	private int isDefault;
}