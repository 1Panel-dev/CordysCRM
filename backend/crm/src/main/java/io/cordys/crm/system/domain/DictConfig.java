package io.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 系统字典配置表;
 */
@Data
@Table(name = "sys_dict_config")
public class DictConfig {

	@Schema(description = "字典类型")
	private String module;

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "是否启用")
	private Boolean enabled;
}
