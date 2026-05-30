package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.contract.constants.ContractPaymentPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Data
public class ContractPaymentPlanUpdateRequest {

	@Size(max = 255)
	private String name;

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Schema(description = "合同ID")
    private String contractId;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "计划状态")
    @EnumValue(enumClass = ContractPaymentPlanStatus.class)
    private String planStatus;

    @Schema(description = "计划回款金额")
    private BigDecimal planAmount;

    @Schema(description = "计划回款时间")
    private Long planEndTime;

    @Schema(description = "模块字段值")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "是否是AI调用")
    private Boolean agentInvoke = false;
}