package io.cordys.crm.opportunity.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Table(name = "opportunity")
public class Opportunity extends BaseModel {

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "客户名称")
	private String name;

	@Schema(description = "金额")
	private BigDecimal amount;

	@Schema(description = "可能性")
	private BigDecimal possible;

	@Schema(description = "意向产品id")
	private String products;

	@Schema(description = "组织id")
	private String organizationId;
}
