package cn.cordys.crm.opportunity.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商机审批前置快照请求体
 * <p>
 * 仅用于审批资源快照({@code approval_resource_snapshot})的写入与回读,
 * 即 {@code getPreUpdateSnapshotData} 落库、{@code revertToSnapshot} 回读,
 * 不作为接口入参, 因此无需 Swagger 注解。
 * <p>
 * 阶段不在商机编辑接口的可变更字段内(编辑见 {@code newOpportunity}, 不会改变阶段),
 * 故驳回/撤回时代理回退所需的编辑前阶段随之存入快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpportunityApprovalSnapshotRequest extends OpportunityUpdateRequest {

    /**
     * 编辑前阶段: 审批驳回/撤回时用于回退阶段。
     */
    private String stage;
}
