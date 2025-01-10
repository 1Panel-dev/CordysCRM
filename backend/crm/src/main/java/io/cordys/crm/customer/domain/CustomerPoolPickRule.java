package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;

public class CustomerPoolPickRule extends BaseModel {

	@Schema(description = "公海池ID")
	private String poolId;

	@Schema(description = "是否限制领取数量")
	private int limitOnNumber;

	@Schema(description = "领取数量")
	private Integer pickNumber;

	@Schema(description = "是否限制前归属人领取")
	private int limitPreOwner;

	@Schema(description = "领取间隔天数")
	private Integer pickIntervalDays;
}
