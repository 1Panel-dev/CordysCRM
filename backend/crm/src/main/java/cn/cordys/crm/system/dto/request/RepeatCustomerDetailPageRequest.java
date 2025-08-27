package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RepeatCustomerDetailPageRequest extends RepeatCustomerPageRequest {
    
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

}
