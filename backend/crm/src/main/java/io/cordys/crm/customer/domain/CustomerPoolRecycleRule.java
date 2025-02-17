package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "customer_pool_recycle_rule")
public class CustomerPoolRecycleRule extends BaseModel {

	@Schema(description = "公海池ID")
	private String poolId;

	@Schema(description = "到期提醒")
	private Boolean expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;

	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "回收条件")
	private String condition;
}
