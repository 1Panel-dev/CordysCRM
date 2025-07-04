package io.cordys.crm.dashboard.domain;

import jakarta.persistence.Table;

import io.cordys.common.domain.BaseModel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;



@Data
@Table(name = "dashboard")
public class Dashboard extends BaseModel {

	@Schema(description = "名称")
	private String name;

	@Schema(description = "三方资源id")
	private String resourceId;

	@Schema(description = "模块id")
	private String dashboardModuleId;

	@Schema(description = "范围")
	private String scopeId;

	@Schema(description = "描述")
	private String description;
}
