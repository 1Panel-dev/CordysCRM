package io.cordys.crm.customer.dto.request;

import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerPoolSaveRequest extends CustomerPool {

	@Schema(description = "领取规则")
	private CustomerPoolPickRule pickRule;
	@Schema(description = "回收规则")
	private CustomerPoolRecycleRule recycleRule;
}
