package cn.cordys.crm.customer.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "customer_pool_hidden_field")
public class CustomerPoolHiddenField {

	@Schema(description = "公海池ID")
	private String poolId;

	@Schema(description = "字段ID")
	private String fieldId;
}
