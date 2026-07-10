package cn.cordys.crm.product.dto.request;

import cn.cordys.common.dto.ExportHeadDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;


@Data
public class ProductExportRequest extends ProductPageRequest {

    @NotBlank
    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "表头信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ExportHeadDTO> headList;
}
