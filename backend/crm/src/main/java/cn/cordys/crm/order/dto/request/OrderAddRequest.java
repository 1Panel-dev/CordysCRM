package cn.cordys.crm.order.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 32)
    @Schema(description = "客户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Size(max = 32)
    @Schema(description = "合同id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contractId;

    @NotBlank(message = "{owner.required}")
    @Size(max = 32)
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;

    @Schema(description = "累计金额")
    private String amount;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "表单配置")
    private ModuleFormConfigDTO moduleFormConfigDTO;

	@Schema(description = "编号")
	private String number;
}
