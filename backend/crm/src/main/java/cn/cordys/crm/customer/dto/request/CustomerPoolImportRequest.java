package cn.cordys.crm.customer.dto.request;

import cn.cordys.crm.system.dto.request.ImportRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerPoolImportRequest extends ImportRequest {

    @Schema(description = "公海ID")
    private String poolId;
}
