package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模块字段配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_module_field")
public class ModuleField extends BaseModel {

	@Schema(description = "所属表单")
	private String formId;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "字段内置Key")
	private String internalKey;

	@Schema(description = "字段类型")
	private String type;

	@Schema(description = "是否移动端")
	private Boolean mobile;

	@Schema(description = "排序")
	private Long pos;
}