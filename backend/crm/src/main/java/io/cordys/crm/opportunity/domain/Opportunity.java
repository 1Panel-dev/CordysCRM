package io.cordys.crm.opportunity.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
@Table(name = "opportunity")
public class Opportunity extends BaseModel {

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "商机名称")
	private String name;

	@Schema(description = "金额")
	private BigDecimal amount;

	@Schema(description = "可能性")
	private BigDecimal possible;

	@Schema(description = "意向产品id")
	private List<String> products;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "商机阶段")
	private String stage;

	@Schema(description = "状态")
	private Boolean status;

	@Schema(description = "联系人")
	private String contactId;

	@Schema(description = "责任人")
	private String owner;

	@Schema(description = "最新跟进人")
	private String follower;

	@Schema(description = "最新跟进时间")
	private Long followTime;

}
