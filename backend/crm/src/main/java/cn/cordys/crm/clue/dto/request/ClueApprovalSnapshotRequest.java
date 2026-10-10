package cn.cordys.crm.clue.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线索审批前置快照请求体
 * <p>
 * 仅用于审批资源快照({@code approval_resource_snapshot})的写入与回读,
 * 即 {@code getPreUpdateSnapshotData} 落库、{@code revertToSnapshot} 回读,
 * 不作为接口入参, 因此无需 Swagger 注解。
 * <p>
 * 线索转移除负责人外还会重置领取时间并把阶段改为跟进中, 这两项不在编辑接口的可变更字段内,
 * 故驳回/撤回时代理回退所需的编辑前取值随之存入快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClueApprovalSnapshotRequest extends ClueUpdateRequest {

    /**
     * 编辑前领取时间: 转移会重置为转移时间, 审批驳回/撤回时用于回退。
     */
    private Long collectionTime;

    /**
     * 编辑前阶段: 转移会把阶段改为跟进中, 审批驳回/撤回时用于回退。
     */
    private String stage;
}
