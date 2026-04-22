package cn.cordys.crm.approval.dto.response;

import cn.cordys.common.dto.condition.CombineSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "条件节点响应")
public class ApprovalNodeConditionResponse extends ApprovalNodeResponse {

    @Schema(description = "条件配置")
    private CombineSearch conditionConfig;
}