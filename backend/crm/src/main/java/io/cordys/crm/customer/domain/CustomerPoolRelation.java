package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "customer_pool_relation")
public class CustomerPoolRelation extends BaseModel {

	@Schema(description = "线索池ID")
	private String poolId;
	@Schema(description = "公海池ID")
	private String customerId;
}
