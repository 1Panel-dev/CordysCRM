package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PoolCustomerBatchRequest {

	@NotNull
	@Schema(description = "客户ID集合列表")
	private List<String> batchIds;

	@NotBlank
	@Schema(description = "公海ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String poolId;
}
