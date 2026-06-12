package cn.cordys.crm.form.dto.request;

import cn.cordys.common.dto.ExportHeadDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomFormDataExportRequest extends CustomFormDataPageRequest {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "表头信息")
    @NotEmpty(message = "{export_head_list_is_empty}")
    private List<ExportHeadDTO> headList;
}
