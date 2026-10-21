package cn.cordys.crm.clue.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class CluePageRequest extends BasePageRequest {
    @Schema(description = "线索池ID{线索池-线索列表时传参}")
    private String poolId;
}
