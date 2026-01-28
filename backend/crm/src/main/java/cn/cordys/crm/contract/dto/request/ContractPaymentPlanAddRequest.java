package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.contract.constants.ContractPaymentPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Data
public class ContractPaymentPlanAddRequest {

  @NotBlank
  @Size(max = 255)
  @Schema(description = "回款计划名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Size(max = 32)
  @NotBlank
  @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String contractId;

  @Size(max = 32)
  @NotBlank
  @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
  private String owner;

  @Size(max = 32)
  @Schema(description = "计划状态", requiredMode = Schema.RequiredMode.REQUIRED)
  @EnumValue(enumClass = ContractPaymentPlanStatus.class)
  private String planStatus;

  private BigDecimal planAmount;

  private Long planEndTime;

  @Schema(description = "模块字段值")
  private List<BaseModuleFieldValue> moduleFields;
}
