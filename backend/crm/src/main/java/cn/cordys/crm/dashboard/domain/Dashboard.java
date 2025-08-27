package cn.cordys.crm.dashboard.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
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

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "同一节点下顺序")
	private Long pos;

	@Schema(description = "范围")
	private String scopeId;

	@Schema(description = "描述")
	private String description;
}
