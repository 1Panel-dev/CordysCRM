package cn.cordys.crm.clue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class ClueBatchTransferRequest {

    @NotEmpty
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "修改负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    @Size(max = 300)
    @Schema(description = "变更说明")
    private String comment;
}
