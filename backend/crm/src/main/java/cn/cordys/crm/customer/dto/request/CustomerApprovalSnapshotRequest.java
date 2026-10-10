package cn.cordys.crm.customer.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客户审批前置快照请求体
 * <p>
 * 仅用于审批资源快照({@code approval_resource_snapshot})的写入与回读,
 * 即 {@code getPreUpdateSnapshotData} 落库、{@code revertToSnapshot} 回读,
 * 不作为接口入参, 因此无需 Swagger 注解。
 * <p>
 * 客户转移会重置领取时间, 且编辑回退时的负责人变更分支会把领取时间覆盖为回退时间,
 * 故转移前的领取时间随之存入快照, 供驳回/撤回时按原值还原。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerApprovalSnapshotRequest extends CustomerUpdateRequest {

    /**
     * 编辑前领取时间: 审批驳回/撤回时用于回退。
     */
    private Long collectionTime;

    /**
     * 编辑前挂在原负责人名下的联系人ID: 审批驳回/撤回时按 id 精确还原。
     * <p>
     * 负责人变更会把这批联系人整批改派, 回退若按"新负责人"反查, 会把本来就挂在
     * 新负责人名下的联系人一并拖回; 记下 id 才能只还原这次真正被改派的那些。
     * 空集合表示确实没有联系人跟着改派, null 表示升级前落库的旧快照(只能近似还原)。
     */
    private List<String> contactIds;
}
