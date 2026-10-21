package cn.cordys.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 审批用户信息，包含用户标识、联系方式、头像及审批结果和原因。
 */
@Data
public class UserApprovalDTO implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "审核结果")
    private String approveResult;

    @Schema(description = "审核原因")
    private String approveReason;

}
