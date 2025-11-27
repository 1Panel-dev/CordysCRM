package cn.cordys.crm.opportunity.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class OpportunityQuotationAddRequest {

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @NotBlank
    @Schema(description = "商机id")
    private String opportunityId;

    @Schema(description = "累计金额")
    private BigDecimal amount;

    @Schema(description = "自定义字段值")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "表单配置")
    private ModuleFormConfigDTO moduleFormConfigDTO;

	@Schema(description = "子产品信息")
	private List<Map<String, Object>> products;
}
