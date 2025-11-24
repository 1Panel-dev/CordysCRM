package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ContractAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 32)
    @Schema(description = "客户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Size(max = 32)
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "表单配置")
    private ModuleFormConfigDTO moduleFormConfigDTO;
}
