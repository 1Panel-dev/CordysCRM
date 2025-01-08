package io.cordys.crm.lead.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "lead_pool_recycle_rule")
public class LeadPoolRecycleRule extends BaseModel {

	@Schema(description = "线索池ID")
	private String poolId;

	@Schema(description = "到期提醒")
	private int expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;

	// TODO: some recycle condition
}
