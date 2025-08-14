package io.cordys.crm.dashboard.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "dashboard_module")
public class DashboardModule extends BaseModel {

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "父节点id")
	private String parentId;

	@Schema(description = "同一节点下顺序")
	private Long pos;
}
