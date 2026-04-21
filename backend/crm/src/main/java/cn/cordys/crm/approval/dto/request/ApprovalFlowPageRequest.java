package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalFlowPageRequest extends BasePageRequest {

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "表单类型")
    private String formType;

    @Schema(description = "启用状态")
    private Boolean enable;
}