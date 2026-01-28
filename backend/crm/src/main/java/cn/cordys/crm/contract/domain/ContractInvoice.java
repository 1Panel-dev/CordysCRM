package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 发票
 *
 * @author jianxing
 * @date 2025-12-29 18:22:59
 */
@Data
@Table(name = "contract_invoice")
public class ContractInvoice extends BaseModel {

  @Schema(description = "发票名称")
  private String name;

  @Schema(description = "合同id")
  private String contractId;

  @Schema(description = "负责人")
  private String owner;

  @Schema(description = "开票金额")
  private BigDecimal amount;

  @Schema(description = "发票类型")
  private String invoiceType;

  @Schema(description = "税率")
  private BigDecimal taxRate;

  @Schema(description = "工商抬头")
  private String businessTitleId;

  @Schema(description = "审核状态")
  private String approvalStatus;

  @Schema(description = "组织id")
  private String organizationId;
}
