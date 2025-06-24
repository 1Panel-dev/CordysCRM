package io.cordys.crm.customer.dto.request;

import io.cordys.common.dto.ExportHeadDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomerExportRequest extends CustomerPageRequest{

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "表头信息")
    private List<ExportHeadDTO> headList;
}
