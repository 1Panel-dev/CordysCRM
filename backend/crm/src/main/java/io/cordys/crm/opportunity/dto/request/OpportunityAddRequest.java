package io.cordys.crm.opportunity.dto.request;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OpportunityAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "商机名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "客户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "意向产品", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> products;

    @Schema(description = "可能性")
    private BigDecimal possible;

    @Size(max = 32)
    @NotBlank
    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactId;

    @Size(max = 32)
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;


    @Schema(description = "结束时间")
    private Long expectedEndTime;


    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

}
