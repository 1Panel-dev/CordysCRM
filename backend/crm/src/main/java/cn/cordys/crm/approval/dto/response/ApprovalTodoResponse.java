package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalTodoResponse {

    @Schema(description = "报价待办")
    private List<ApprovalTodoItemResponse> quotation;

    @Schema(description = "合同待办")
    private List<ApprovalTodoItemResponse> contract;

    @Schema(description = "订单待办")
    private List<ApprovalTodoItemResponse> order;

    @Schema(description = "发票待办")
    private List<ApprovalTodoItemResponse> invoice;
}
