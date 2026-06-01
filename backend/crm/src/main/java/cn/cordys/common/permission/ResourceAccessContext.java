package cn.cordys.common.permission;

import lombok.Data;

/**
 * 资源访问上下文，包含权限校验所需的资源信息
 */
@Data
public class ResourceAccessContext {

    /**
     * 资源负责人ID
     */
    private String ownerId;

    /**
     * 审批状态
     */
    private String approvalStatus;
}
