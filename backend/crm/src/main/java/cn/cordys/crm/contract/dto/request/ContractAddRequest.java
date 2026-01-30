package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContractAddRequest {

    @NotBlank(message = "{contract.name.required}")
    @Size(max = 255)
    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{contract.customer.required}")
    @Size(max = 32)
    @Schema(description = "客户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @NotBlank(message = "{owner.required}")
    @Size(max = 32)
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    @Schema(description = "累计金额")
    private String amount;

    @Schema(description = "合同开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long startTime;

    @Schema(description = "合同结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long endTime;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "表单配置")
    private ModuleFormConfigDTO moduleFormConfigDTO;

    @Schema(description = "子产品信息")
    private List<Map<String, Object>> products;
}
