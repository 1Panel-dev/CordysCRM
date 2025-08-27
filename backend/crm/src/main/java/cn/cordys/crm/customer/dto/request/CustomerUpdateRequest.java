package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerUpdateRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Size(max = 255)
    @Schema(description = "客户名称")
    private String name;

    @Size(max = 32)
    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}