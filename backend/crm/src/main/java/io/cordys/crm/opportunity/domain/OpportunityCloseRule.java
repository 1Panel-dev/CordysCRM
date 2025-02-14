package io.cordys.crm.opportunity.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "opportunity_close_rule")
public class OpportunityCloseRule extends BaseModel {

	@Schema(description = "规则名称")
	private String name;

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "管理员ID")
	private String ownerId;

	@Schema(description = "范围ID")
	private String scopeId;

	@Schema(description = "启用/禁用")
	private Boolean enable;

	@Schema(description = "自动回收")
	private Boolean auto;

	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "回收条件")
	private String condition;

	@Schema(description = "到期提醒")
	private Boolean expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;
}
