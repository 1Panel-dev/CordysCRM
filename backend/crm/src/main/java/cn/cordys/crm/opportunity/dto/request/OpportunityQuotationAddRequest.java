package cn.cordys.crm.opportunity.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OpportunityQuotationAddRequest {

    @NotBlank(message = "{opportunity.quotation.name.required}")
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{opportunity.required}")
    @Schema(description = "商机id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String opportunityId;

    @Schema(description = "有效期至", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long untilTime;

    @Schema(description = "累计金额")
    private String amount;

    @NotEmpty(message = "{opportunity.quotation.field.required}")
    @Schema(description = "自定义字段值", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<BaseModuleFieldValue> moduleFields;

    @NotNull(message = "{opportunity.quotation.form.config.required}")
    @Schema(description = "表单配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private ModuleFormConfigDTO moduleFormConfigDTO;

    @Schema(description = "子产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Map<String, Object>> products;
}
