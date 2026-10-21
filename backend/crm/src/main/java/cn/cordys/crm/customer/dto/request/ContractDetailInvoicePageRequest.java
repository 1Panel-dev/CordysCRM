package cn.cordys.crm.customer.dto.request;

import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class ContractDetailInvoicePageRequest extends ContractInvoicePageRequest {
    @NotBlank
    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contractId;

    @Override
    public String getContractId() {
        return contractId;
    }
}
