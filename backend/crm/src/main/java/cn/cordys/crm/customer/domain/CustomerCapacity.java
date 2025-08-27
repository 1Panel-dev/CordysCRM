package cn.cordys.crm.customer.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "customer_capacity")
public class CustomerCapacity extends BaseModel {

	@Schema(description = "组织架构ID")
	private String organizationId;

	@Schema(description = "范围ID")
	private String scopeId;

	@Schema(description = "库容")
	private Integer capacity;

	@Schema(description = "过滤条件")
	private String filter;
}
