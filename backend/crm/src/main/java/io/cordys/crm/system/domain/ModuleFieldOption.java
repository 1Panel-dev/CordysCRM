package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模块字段选项值配置(部分选项字段);
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sys_module_field_option")
public class ModuleFieldOption extends BaseModel {

	@Schema(description = "所属模块字段ID")
	private String fieldId;

	@Schema(description = "选项值")
	private String fieldKey;

	@Schema(description = "选项文本")
	private String fieldLabel;
}