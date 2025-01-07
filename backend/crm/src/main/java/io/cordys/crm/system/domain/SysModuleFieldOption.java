package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * 模块字段选项值配置(部分选项字段);
 */
@Data
public class SysModuleFieldOption extends BaseModel {

	@Schema(description = "所属模块字段ID")
	private String fieldId;

	@Schema(description = "选项值")
	private String fieldKey;

	@Schema(description = "选项文本")
	private String filedLabel;
}