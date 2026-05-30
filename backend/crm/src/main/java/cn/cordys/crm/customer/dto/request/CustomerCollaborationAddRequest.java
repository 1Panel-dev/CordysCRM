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
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerCollaborationAddRequest {

    @Size(max = 32)
    @NotBlank
    @Schema(description = "客户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "人员", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @NotNull
    @Schema(description = "协作类型(只读 READ_ONLY / 协作 COLLABORATION)", requiredMode = Schema.RequiredMode.REQUIRED)
    @EnumValue(enumClass = CustomerCollaborationType.class)
    private String collaborationType;
}