package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PoolCustomerAssignRequest {

	@Schema(description = "客户ID")
	private String customerId;
	@Schema(description = "分配用户ID")
	private String assignUserId;
}
