package cn.cordys.common.permission;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 资源访问上下文提供者
 * <p>
 * 各业务模块实现此接口，提供资源负责人和审批状态信息
 */
public interface ResourceAccessContextProvider {

    /**
     * 返回此提供者处理的表单类型，如 "order"、"contract"
     */
    String getFormType();

    /**
     * 是否按负责人校验数据范围。无负责人、按组织共享的资源可显式关闭。
     */
    default boolean requiresOwner() {
        return true;
    }

    /**
     * 批量操作是否校验负责人数据范围，默认沿用单条资源策略。
     */
    default boolean requiresBatchOwner() {
        return requiresOwner();
    }

    /**
     * 返回当前组织中存在的资源ID；组织共享资源应覆盖此方法。
     */
    default Set<String> batchGetResourceIds(List<String> resourceIds, String orgId) {
        return batchGetOwnerIds(resourceIds, orgId).keySet();
    }

    /**
     * 获取资源访问上下文
     *
     * @param resourceId 资源ID
     * @param orgId      组织ID
     * @return 资源访问上下文，资源不存在时返回 null
     */
    ResourceAccessContext getAccessContext(String resourceId, String orgId);

    /**
     * 批量获取资源负责人ID
     * <p>
     * 默认逐条调用 getAccessContext，子类可重写以优化为批量查询
     *
     * @param resourceIds 资源ID列表
     * @param orgId       组织ID
     * @return 资源ID -> 负责人ID 的映射，不包含负责人为空的资源
     */
    Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId);
}
