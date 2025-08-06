package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportHeadDTO {

    @Schema(description = "key")
    private String key;
    @Schema(description = "表头名称")
    private String title;
}
