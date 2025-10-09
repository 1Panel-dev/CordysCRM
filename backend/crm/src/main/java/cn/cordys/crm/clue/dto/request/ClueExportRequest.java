package cn.cordys.crm.clue.dto.request;

import cn.cordys.common.dto.ExportHeadDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClueExportRequest extends CluePageRequest {

    @NotBlank
    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "表头信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ExportHeadDTO> headList;
}
