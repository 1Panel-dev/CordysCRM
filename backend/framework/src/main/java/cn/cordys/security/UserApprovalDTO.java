package cn.cordys.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * <p>用户数据传输对象，用于传输用户信息，包含其他平台对接信息和头像。</p>
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
