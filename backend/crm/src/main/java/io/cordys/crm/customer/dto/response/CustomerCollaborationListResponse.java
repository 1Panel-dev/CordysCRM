package io.cordys.crm.customer.dto.response;

import io.cordys.crm.customer.domain.CustomerCollaboration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerCollaborationListResponse extends CustomerCollaboration {
    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;
}
