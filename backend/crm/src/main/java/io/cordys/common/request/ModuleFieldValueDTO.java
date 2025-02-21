package io.cordys.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: jianxing
 * @CreateTime: 2025-02-10  14:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleFieldValueDTO {
    @Schema(description = "字段ID")
    private String id;
    @Schema(description = "字段值")
    private Object value;
}
