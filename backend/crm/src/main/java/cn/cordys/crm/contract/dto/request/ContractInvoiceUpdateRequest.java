package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-12-29 18:22:59
 */
@Data
public class ContractInvoiceUpdateRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Schema(description = "编号")
    private String number;

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

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "表单配置")
    private ModuleFormConfigDTO moduleFormConfigDTO;
}