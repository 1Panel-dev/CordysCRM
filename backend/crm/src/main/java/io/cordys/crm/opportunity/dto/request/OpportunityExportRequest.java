package io.cordys.crm.opportunity.dto.request;

import io.cordys.crm.opportunity.dto.ExportHeadDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityExportRequest extends OpportunityPageRequest {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "表头信息")
    private List<ExportHeadDTO> headList;
}
