package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ContractApprovalBatchRequest {

    @NotEmpty
    @Schema(description = "ID集合")
    private List<String> ids;

    @NotBlank
    @Schema(description = "状态")
    private String approvalStatus;
}
