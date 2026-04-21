package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "条件节点请求")
public class ApprovalNodeConditionRequest extends ApprovalNodeRequest {

    @Schema(description = "条件表达式列表")
    private List<ConditionRule> rules;

    @Data
    public static class ConditionRule {
        @Schema(description = "字段名")
        private String field;
        @Schema(description = "操作符")
        private String operator;
        @Schema(description = "值")
        private String value;
    }
}