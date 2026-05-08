package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalProcessedPageRequest extends BasePageRequest {

    @Schema(description = "资源名称（报价/合同/订单/发票）")
    private String resourceName;
}
