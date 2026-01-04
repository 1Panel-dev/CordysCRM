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
public class ContractInvoiceAddRequest {
    @Size(max = 50)
    @NotBlank
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String number;

    @Size(max = 32)
    @NotBlank
    @Schema(description = "合同id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contractId;

    @Size(max = 32)
    @NotBlank
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    private BigDecimal amount;

    @Size(max = 50)
    private String invoiceType;

    private BigDecimal taxRate;

    @Size(max = 50)
    private String businessTitleId;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "表单配置")
    private ModuleFormConfigDTO moduleFormConfigDTO;
}