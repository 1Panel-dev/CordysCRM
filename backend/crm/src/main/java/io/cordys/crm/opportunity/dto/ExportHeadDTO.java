package io.cordys.crm.opportunity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExportHeadDTO {

    @Schema(description = "key")
    private String key;
    @Schema(description = "表头名称")
    private String title;
}
