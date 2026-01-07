package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同回款计划
 * 
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Data
@Table(name = "contract_payment_plan")
public class ContractPaymentPlan extends BaseModel {

	@Schema(description = "回款计划名称")
	private String name;

	@Schema(description = "合同ID")
	private String contractId;

	@Schema(description = "负责人")
	private String owner;

	@Schema(description = "计划状态")
	private String planStatus;

	@Schema(description = "计划回款金额")
	private BigDecimal planAmount;

	@Schema(description = "计划回款时间")
	private Long planEndTime;

	@Schema(description = "组织id")
	private String organizationId;
}
