package cn.cordys.crm.approval.dto.response;

import cn.cordys.security.UserApprovalDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ResourceApprovalResponse {

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "最终审核状态")
    private String approveStatus;

    @Schema(description = "用户审核结果")
    private List<UserApprovalDTO> approveUserList;

}
