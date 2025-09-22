package cn.cordys.crm.clue.dto.response;

import cn.cordys.crm.clue.domain.ClueOwner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueOwnerListResponse extends ClueOwner {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "责任人名称")
    private String ownerName;

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "回收原因名称")
    private String reasonName;

}
