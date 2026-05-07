package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalTodoPageRequest extends BasePageRequest {

    @Schema(description = "资源类型过滤：ALL/QUOTATION/CONTRACT/ORDER/INVOICE")
    private String resourceType = "ALL";
}
