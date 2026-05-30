package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalTodoCountResponse {

    @Schema(description = "待我审批总数")
    private Integer total;

    @Schema(description = "报价待审批数量")
    private Integer quotation;

    @Schema(description = "合同待审批数量")
    private Integer contract;

    @Schema(description = "订单待审批数量")
    private Integer order;

    @Schema(description = "发票待审批数量")
    private Integer invoice;
}
