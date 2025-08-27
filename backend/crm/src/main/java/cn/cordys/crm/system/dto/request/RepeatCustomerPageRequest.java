package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RepeatCustomerPageRequest extends BasePageRequest {

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
