package io.cordys.crm.dashboard.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "dashboard_collection")
public class DashboardCollection extends BaseModel {

	@Schema(description = "用户id")
	private String userId;

	@Schema(description = "仪表板id")
	private String dashboardId;
}
