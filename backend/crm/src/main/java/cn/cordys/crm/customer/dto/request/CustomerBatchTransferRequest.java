package cn.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerBatchTransferRequest {

    @NotEmpty
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "修改负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;
}