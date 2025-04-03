package io.cordys.crm.follow.dto.request;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FollowUpPlanAddRequest {

    @Size(max = 32)
    @Schema(description = "客户id")
    private String customerId;

    @Size(max = 32)
    @Schema(description = "商机id")
    private String opportunityId;

    @Size(max = 32)
    @NotBlank
    @Schema(description = "类型:CUSTOMER/CLUE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Size(max = 32)
    @Schema(description = "线索id")
    private String clueId;

    @Size(max = 1000)
    @Schema(description = "预计沟通内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 32)
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    @Size(max = 32)
    @NotBlank
    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactId;

    @Schema(description = "预计开始时间")
    private Long estimatedTime;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}
