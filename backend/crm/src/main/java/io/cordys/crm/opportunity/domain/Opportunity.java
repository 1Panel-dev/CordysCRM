package io.cordys.crm.opportunity.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
	private String productId;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "商机阶段")
	private String stage;

	@Schema(description = "状态")
	private Boolean status;

	@Schema(description = "联系人")
	private String contactId;

	@Schema(description = "联系人")
	private String owner;

	@Schema(description = "剩余归属天数")
	private String reserveDate;
}
