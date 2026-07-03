package cn.cordys.crm.clue.dto.request;

import cn.cordys.crm.system.dto.request.ImportRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CluePoolImportRequest extends ImportRequest {

    @Schema(description = "线索池ID")
    private String poolId;
}
