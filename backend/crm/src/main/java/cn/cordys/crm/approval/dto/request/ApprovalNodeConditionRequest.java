package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.dto.condition.CombineSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "条件节点请求")
public class ApprovalNodeConditionRequest extends ApprovalNodeRequest {

    @Schema(description = "条件配置")
    private CombineSearch conditionConfig;
}