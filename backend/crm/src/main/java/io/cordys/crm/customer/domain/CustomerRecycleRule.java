package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerRecycleRule extends BaseModel {

	@Schema(description = "公海池ID")
	private String poolId;

	@Schema(description = "到期提醒")
	private int expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;
}
