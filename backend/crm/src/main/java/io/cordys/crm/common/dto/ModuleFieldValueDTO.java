package io.cordys.crm.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-02-10  14:31
 */
@Data
public class ModuleFieldValueDTO {
    @Schema(description = "字段ID")
    private String id;
    @Schema(description = "字段值")
    private String value;
}
