package cn.cordys.common.permission;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

/**
 * 资源权限校验服务
 * <p>
 * 校验逻辑：(角色权限位 && 角色数据权限 && 审批状态权限)
 */
@Service
public class ResourcePermissionService {

    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;

    /**
     * 仅校验角色权限位
     */
    public void checkPermission(String permission) {
        if (!PermissionUtils.hasPermission(permission)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    /**
     * 完整权限校验：(角色权限位 && 角色数据权限 && 审批状态权限)
     *
     * @param permission 权限码
     * @param resourceId 资源ID
     * @param formType   表单类型
     * @param userId     当前用户ID
     * @param orgId      组织ID
     */
    public void checkResourcePermission(String permission, String resourceId, String formType, String userId, String orgId) {
        if (StringUtils.isBlank(resourceId)) {
            checkPermission(permission);
            return;
        }

        // 从HTTP请求中获取待办任务ID
        String approvalTaskId = resolveApprovalTaskId();
        // 如果传了待办任务ID，只校验当前用户是否是该待办的所有人
        if (StringUtils.isNotBlank(approvalTaskId)) {
            if (!isTaskOwner(approvalTaskId, resourceId, userId)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            return;
        }

        ResourceAccessContextProvider provider = getProvider(formType);
        ResourceAccessContext context = provider != null ? provider.getAccessContext(resourceId, orgId) : null;
        boolean permitted = checkRoleAndDataAndStatusPermission(permission, formType, userId, orgId, context);

        if (!permitted) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    /**
     * 校验当前用户是否是指定待办任务的所有人，且该任务关联的资源与请求资源一致
     */
    private boolean isTaskOwner(String approvalTaskId, String resourceId, String userId) {
        if (StringUtils.isBlank(approvalTaskId) || StringUtils.isBlank(userId)) {
            return false;
        }

        ApprovalTask task = approvalTaskMapper.selectByPrimaryKey(approvalTaskId);
        if (task == null || !userId.equals(task.getApproverId())) {
            return false;
        }

        // 校验任务关联的审批实例资源是否与当前请求的资源一致
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(task.getInstanceId());
        return instance != null && resourceId.equals(instance.getResourceId());
    }

    /**
     * 从HTTP请求中提取待办任务ID
     * 优先从请求头 X-Pending-Task-Id 获取，其次从查询参数 pendingTaskId 获取
     */
    private String resolveApprovalTaskId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String pendingTaskId = request.getHeader("Approval-Task-Id");
        if (pendingTaskId == null || pendingTaskId.isBlank()) {
            pendingTaskId = request.getParameter("approvalTaskId");
        }
        return (pendingTaskId != null && !pendingTaskId.isBlank()) ? pendingTaskId.trim() : null;
    }

    /**
     * 批量权限校验：仅校验角色权限位 + 数据权限（跳过审批状态权限）
     */
    public void checkBatchResourcePermission(String permission, List<String> resourceIds, String formType, String userId, String orgId) {
        if (!PermissionUtils.hasPermission(permission)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        if (StringUtils.isNotBlank(formType) && CollectionUtils.isNotEmpty(resourceIds)) {
            ResourceAccessContextProvider provider = getProvider(formType);
            if (provider == null) {
                return;
            }
            Map<String, String> ownerMap = provider.batchGetOwnerIds(resourceIds, orgId);
            List<String> ownerIds = ownerMap.values().stream()
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toList();
            if (!ownerIds.isEmpty()) {
                dataScopeService.checkDataPermission(userId, orgId, ownerIds, permission);
            }
        }
    }

    /**
     * 校验角色权限位 + 数据权限 + 审批状态权限：(1 && 2 && 3)
     */
    private boolean checkRoleAndDataAndStatusPermission(String permission, String formType,
                                                        String userId, String orgId, ResourceAccessContext context) {

        String ownerId = context != null ? context.getOwnerId() : null;
        String approvalStatus = context != null ? context.getApprovalStatus() : null;

        // Check 1: 角色权限位
        if (!PermissionUtils.hasPermission(permission)) {
            return false;
        }

        // Check 2: 角色的数据权限
        if (ownerId != null && !dataScopeService.hasDataPermission(userId, orgId, ownerId, permission)) {
            return false;
        }

        // Check 3: 审批流的状态权限
        if (StringUtils.isNotBlank(formType) && !checkStatusPermission(permission, formType, approvalStatus, orgId)) {
            return false;
        }

        return true;
    }

    /**
     * 校验审批状态权限
     * 如果审批状态为 NONE 或无审批流配置，默认允许
     */
    private boolean checkStatusPermission(String permission, String formType, String approvalStatus, String orgId) {
        if (StringUtils.isBlank(approvalStatus) || ApprovalStatus.NONE.name().equals(approvalStatus)) {
            return true;
        }

        // 查询状态权限配置
        var setting = approvalFlowService.getStatusPermissionsByFormType(formType, orgId);
        if (setting == null || CollectionUtils.isEmpty(setting.getStatusPermissions())) {
            return true;
        }

        // 查找当前审批状态对应的权限配置
        for (var sp : setting.getStatusPermissions()) {
            if (permission.equals(sp.getPermission()) && approvalStatus.equals(sp.getApprovalStatus())) {
                return Boolean.TRUE.equals(sp.getEnabled());
            }
        }

        // 未找到对应配置，默认允许
        return true;
    }

    /**
     * 根据 formType 获取对应的 ResourceAccessContextProvider
     */
    private ResourceAccessContextProvider getProvider(String formType) {
        if (StringUtils.isBlank(formType)) {
            return null;
        }
        Map<String, ResourceAccessContextProvider> providers = cn.cordys.common.util.CommonBeanFactory
                .getBeansOfType(ResourceAccessContextProvider.class);
        if (providers == null) {
            return null;
        }
        return providers.values().stream()
                .filter(p -> formType.equals(p.getFormType()))
                .findFirst()
                .orElse(null);
    }
}
