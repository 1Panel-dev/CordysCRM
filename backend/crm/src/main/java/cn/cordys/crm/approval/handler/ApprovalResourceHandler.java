package cn.cordys.crm.approval.handler;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;

/**
 * 审批资源处理接口
 * 各需要审批的业务资源服务实现此接口，替代反射调用
 */
public interface ApprovalResourceHandler {

    /**
     * 获取该处理器对应的表单类型
     */
    FormKey getFormKey();

    /**
     * DELETE审批通过后执行删除操作
     *
     * @param resourceId     资源ID
     * @param userId         操作人ID
     * @param organizationId 组织ID
     */
    void delete(String resourceId, String userId, String organizationId);

    /**
     * 更新业务快照审批状态
     *
     * @param param 参数
     */
    void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param);

    /**
     * 审批后置字段更新
     *
     * @param param 参数
     */
    void updateApprovalPostField(ResourceApprovalPostUpdateParam param);

    /**
     * 使用快照数据回退资源（回退时会记录编辑日志，但跳过审批）
     *
     * @param resourceId   资源ID
     * @param userId       操作人ID
     * @param orgId        组织ID
     * @param snapshotData 编辑前资源数据快照(JSON)，为 update 请求参数的 JSON 序列化
     */
    void revertToSnapshot(String resourceId, String userId, String orgId, String snapshotData);

    /**
     * 获取编辑前的资源数据快照（用于审批驳回/撤回时回退）
     * 从数据库查询当前完整数据，组装成更新请求参数格式并返回 JSON
     *
     * @param resourceId 资源ID
     * @param userId     操作人ID
     * @param orgId      组织ID
     * @return 快照数据 JSON，无需快照时返回 null
     */
    String getPreUpdateSnapshotData(String resourceId, String userId, String orgId);
}
