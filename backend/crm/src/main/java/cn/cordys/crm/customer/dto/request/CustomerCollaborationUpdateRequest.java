package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class CustomerCollaborationUpdateRequest {
    @Size(max = 32)
    @NotBlank
    @Schema(description = "客户协作人id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotNull
    @Schema(description = "协作类型(只读 READ_ONLY / 仅客户信息 COLLABORATION / 共同跟进 JOINT_FOLLOW)", requiredMode = Schema.RequiredMode.REQUIRED)
    @EnumValue(enumClass = CustomerCollaborationType.class)
    private String collaborationType;

    @Schema(description = "有效期截止时间，留空为长期有效")
    private Long expireTime;
}
