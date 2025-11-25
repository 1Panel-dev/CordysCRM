package cn.cordys.crm.clue.dto.request;

import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ContractDetailPaymentPlanPageRequest extends ContractPaymentPlanPageRequest {

    @NotBlank
    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contractId;

    @Override
    public String getContractId() {
        return contractId;
    }
}
