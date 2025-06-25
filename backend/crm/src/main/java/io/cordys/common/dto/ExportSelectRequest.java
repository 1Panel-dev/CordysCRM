package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ExportSelectRequest {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "表头信息")
    private List<ExportHeadDTO> headList;

    @Schema(description = "勾选的数据id集合")
    private List<String> ids;
}
