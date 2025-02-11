package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_module")
public class Module extends BaseModel {

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "模块KEY")
	private String moduleKey;

	@Schema(description = "启用/禁用")
	private Boolean enable;

	@Schema(description = "自定义排序")
	private Long pos;
}
