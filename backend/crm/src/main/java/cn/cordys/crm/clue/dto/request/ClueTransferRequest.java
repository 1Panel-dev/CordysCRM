package cn.cordys.crm.clue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单个转移线索
 */
@Data
public class ClueTransferRequest {

    @NotBlank
    @Size(max = 32)
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "修改负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    /**
     * 提交人填写的变更说明, 追加在系统生成的审批说明之后;
     * 上限与前端提交编辑变更说明时的输入长度(maxlength: 300)保持一致
     */
    @Size(max = 300)
    @Schema(description = "变更说明")
    private String comment;
}
