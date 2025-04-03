package io.cordys.crm.clue.dto.request;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "客户名称")
    private String name;

    @Size(max = 32)
    @Schema(description = "负责人")
    private String owner;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "联系人名称")
    private String contact;

    @NotBlank
    @Size(max = 30)
    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "模块字段值")
    private List<BaseModuleFieldValue> moduleFields;
}