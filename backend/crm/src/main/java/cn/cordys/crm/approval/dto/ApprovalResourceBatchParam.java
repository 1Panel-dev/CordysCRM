package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalResourceBatchParam {

    @NotBlank
    @Schema(description = "资源表单")
    private String formKey;

    @NotEmpty
    @Schema(description = "资源ID集合")
    private List<String> resourceIds;
}
