package cn.cordys.common.permission;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源权限校验服务
 * <p>
 * 校验逻辑：(角色权限位 &amp;&amp; 角色数据权限 &amp;&amp; 审批状态权限)
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
    @Resource(name = "extApprovalCommentMapper")
    private ExtApprovalCommentMapper extApprovalCommentMapper;

    @Resource
    private List<ResourceAccessContextProvider> contextProviders;
    private Map<String, ResourceAccessContextProvider> providersByFormType = Map.of();

    @PostConstruct
    void initializeProviders() {
        Map<String, ResourceAccessContextProvider> registry = new HashMap<>();
        for (ResourceAccessContextProvider provider : contextProviders) {
            String formType = provider.getFormType();
            if (StringUtils.isBlank(formType) || registry.putIfAbsent(formType, provider) != null) {
                throw new IllegalStateException("Invalid or duplicate resource permission provider: " + formType);
            }
        }
        providersByFormType = Map.copyOf(registry);
    }

    /**
     * 仅校验角色权限位
     */
    public void checkPermission(String permission) {
        if (!PermissionUtils.hasPermission(permission)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    /**
     * 完整权限校验：(角色权限位 &amp;&amp; 角色数据权限 &amp;&amp; 审批状态权限)
     *
     * @param permission 权限码
     * @param resourceId 资源ID
     * @param formType   表单类型
     * @param userId     当前用户ID
     * @param orgId      组织ID
     */
    public void checkResourcePermission(String permission, String resourceId, String formType, String userId,
            String orgId) {
        if (StringUtils.isBlank(resourceId)) {
            checkPermission(permission);
            return;
        }

        // 评论@只授权当前GET请求读取该实例对应的业务数据。
        if (permission != null && permission.endsWith(":READ")
                && hasApprovalCommentMentionPermission(resourceId, userId, orgId, formType)) {
            return;
        }

        // 从HTTP请求中获取待办任务ID
        String approvalTaskId = resolveApprovalTaskId();
        if (StringUtils.isNotBlank(approvalTaskId)) {
            // 只有当前用户处理中(待审批)的任务才能作为权限来源, 已完成/已失效的任务不得继续放权
            if (!isTaskOwner(approvalTaskId, resourceId, userId)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            // 审批待办只能替代审批过程中查看、编辑资源的权限;
            // 删除、转移等业务操作仍须按角色权限位和数据权限校验
            if (isApprovalTaskReviewPermission(permission)) {
                return;
            }
        }

        checkResourcePermissionWithoutApprovalTask(permission, resourceId, formType, userId, orgId);
    }

    /**
     * 按普通业务权限校验资源，审批待办授权不能替代转换等业务操作的权限。
     *
     * @param permission 权限码
     * @param resourceId 资源ID，不能为空
     * @param formType   表单类型
     * @param userId     当前用户ID
     * @param orgId      组织ID
     */
    public void checkResourcePermissionWithoutApprovalTask(String permission, String resourceId, String formType,
            String userId, String orgId) {
        if (StringUtils.isBlank(resourceId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        ResourceAccessContextProvider provider = getProvider(formType);
        ResourceAccessContext context = provider != null ? provider.getAccessContext(resourceId, orgId) : null;
        boolean permitted = checkRoleAndDataAndStatusPermission(permission, resourceId, formType, userId, orgId,
                context, provider);

        if (!permitted) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    public boolean hasApprovalTaskPermission(String resourceId, String userId) {
        // 从HTTP请求中获取待办任务ID
        String approvalTaskId = resolveApprovalTaskId();
        if (StringUtils.isNotBlank(approvalTaskId) && isTaskOwner(approvalTaskId, resourceId, userId)) {
            return true;
        }
        return false;
    }

    public boolean hasApprovalViewPermission(String resourceId, String userId, String orgId) {
        return hasApprovalViewPermission(resourceId, userId, orgId, null);
    }

    public boolean hasApprovalViewPermission(String resourceId, String userId, String orgId, String formType) {
        return hasApprovalTaskPermission(resourceId, userId)
                || hasApprovalCommentMentionPermission(resourceId, userId, orgId, formType);
    }

    public boolean hasApprovalCommentMentionPermission(String resourceId, String userId, String orgId) {
        return hasApprovalCommentMentionPermission(resourceId, userId, orgId, null);
    }

    private boolean hasApprovalCommentMentionPermission(String resourceId, String userId, String orgId, String formType) {
        if (StringUtils.isBlank(resourceId)) {
            return false;
        }
        ApprovalInstance instance = getApprovalCommentMentionInstance(userId, orgId);
        return instance != null && resourceId.equals(instance.getResourceId())
                && (StringUtils.isBlank(formType) || formType.equals(instance.getType()));
    }

    public boolean hasApprovalCommentMentionFormPermission(String formId, String userId, String orgId) {
        if (StringUtils.isBlank(formId)) {
            return false;
        }
        ApprovalInstance instance = getApprovalCommentMentionInstance(userId, orgId);
        return instance != null && formId.equals(instance.getType()) && FormKey.ofKey(instance.getType()) == null;
    }

    private ApprovalInstance getApprovalCommentMentionInstance(String userId, String orgId) {
        if (StringUtils.isAnyBlank(userId, orgId)
                || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String mentionId = StringUtils.trimToNull(request.getHeader("Approval-Comment-Mention-Id"));
        if (mentionId == null) {
            mentionId = StringUtils.trimToNull(request.getParameter("commentMentionId"));
        }
        return mentionId == null ? null : extApprovalCommentMapper.selectMentionInstance(mentionId, userId, orgId);
    }

    /**
     * 校验当前用户是否是指定待办任务的所有人，且该任务关联的资源与请求资源一致
     * <p>
     * 能够查看历史的审批任务
     */
    private boolean isTaskOwner(String approvalTaskId, String resourceId, String userId) {
        if (StringUtils.isBlank(approvalTaskId) || StringUtils.isBlank(userId)) {
            return false;
        }

        ApprovalTask task = approvalTaskMapper.selectByPrimaryKey(approvalTaskId);
        if (task == null || !userId.equals(task.getApproverId())) {
            return false;
        }

        if (StringUtils.isBlank(task.getInstanceId())) {
            return false;
        }

        // 校验任务关联的审批实例资源是否与当前请求的资源一致
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(task.getInstanceId());
        if (instance == null || !resourceId.equals(instance.getResourceId())) {
            return false;
        }

        return true;
    }

    /**
     * 审批待办可以替代的资源权限：查看(READ)与编辑(UPDATE)。
     * 其他操作(删除、转移、回收等)不能凭待办 ID 跳过正常的权限校验。
     */
    private boolean isApprovalTaskReviewPermission(String permission) {
        return permission != null && (permission.endsWith(":READ") || permission.endsWith(":UPDATE"));
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
    public void checkBatchResourcePermission(String permission, List<String> resourceIds, String formType,
            String userId, String orgId) {
        if (!PermissionUtils.hasPermission(permission)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        if (StringUtils.isNotBlank(formType) && CollectionUtils.isNotEmpty(resourceIds)) {
            ResourceAccessContextProvider provider = getProvider(formType);
            if (!provider.requiresBatchOwner()) {
                var existingIds = provider.batchGetResourceIds(resourceIds, orgId);
                if (existingIds == null || resourceIds.stream()
                        .anyMatch(id -> StringUtils.isBlank(id) || !existingIds.contains(id))) {
                    throw new GenericException(CrmHttpResultCode.FORBIDDEN);
                }
                return;
            }
            Map<String, String> ownerMap = provider.batchGetOwnerIds(resourceIds, orgId);
            if (ownerMap == null || resourceIds.stream()
                    .anyMatch(id -> StringUtils.isBlank(id) || StringUtils.isBlank(ownerMap.get(id)))) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            List<String> ownerIds = resourceIds.stream().map(ownerMap::get)
                    .distinct()
                    .toList();
            if (!ownerIds.isEmpty()) {
                dataScopeService.checkDataPermission(userId, orgId, ownerIds, permission);
            }
        }
    }

    /**
     * 校验角色权限位 + 数据权限 + 审批状态权限：(1 &amp;&amp; 2 &amp;&amp; 3)
     */
    private boolean checkRoleAndDataAndStatusPermission(String permission, String resourceId, String formType,
            String userId, String orgId, ResourceAccessContext context,
            ResourceAccessContextProvider provider) {
        boolean requiresOwner = provider == null || provider.requiresOwner();
        // 资源必须存在；只有明确声明为组织共享的资源才允许没有负责人。
        if (StringUtils.isNotBlank(formType)
                && (context == null || (requiresOwner && StringUtils.isBlank(context.getOwnerId())))) {
            return false;
        }
        String ownerId = context != null ? context.getOwnerId() : null;
        String approvalStatus = context != null ? context.getApprovalStatus() : null;

        // Check 1: 角色权限位
        if (!PermissionUtils.hasPermission(permission)) {
            return false;
        }

        // 数据范围不足时允许资源提供者补充授权，后续仍须校验审批状态。
        if (requiresOwner && ownerId != null && !dataScopeService.hasDataPermission(userId, orgId, ownerId, permission)
                && (provider == null || !provider.hasAdditionalDataPermission(permission, resourceId, userId))) {
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
        ResourceAccessContextProvider provider = providersByFormType.get(formType);
        if (provider == null) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        return provider;
    }
}
