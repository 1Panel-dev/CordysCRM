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
}
