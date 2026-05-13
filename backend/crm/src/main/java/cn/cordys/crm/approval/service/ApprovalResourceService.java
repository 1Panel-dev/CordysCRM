package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
import cn.cordys.crm.approval.dto.ApprovalResourceRevokeParam;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.UserApprovalDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApprovalResourceService {

    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ModuleFormService formService;

    /**
     * 开启的审批流表单表格映射
     */
    private static final Map<String, String> FORM_APPROVAL_TABLE = new HashMap<>(4);

    static {
        FORM_APPROVAL_TABLE.put(FormKey.QUOTATION.getKey(), "opportunity_quotation");
        FORM_APPROVAL_TABLE.put(FormKey.CONTRACT.getKey(), "contract");
        FORM_APPROVAL_TABLE.put(FormKey.INVOICE.getKey(), "contract_invoice");
        FORM_APPROVAL_TABLE.put(FormKey.ORDER.getKey(), "sales_order");
    }

    public ResourceApprovalResponse resourceDetail(String resourceId) {
        // 初始化响应对象，默认返回空审核人列表。
        ResourceApprovalResponse response = new ResourceApprovalResponse();
        response.setResourceId(resourceId);
        response.setApproveUserList(Collections.emptyList());
        if (StringUtils.isBlank(resourceId)) {
            return response;
        }

        // 查询资源最近一次审批实例。
        LambdaQueryWrapper<ApprovalInstance> instanceWrapper = new LambdaQueryWrapper<>();
        instanceWrapper.eq(ApprovalInstance::getResourceId, resourceId)
                .orderByDesc(ApprovalInstance::getSubmitTime);
        List<ApprovalInstance> instances = approvalInstanceMapper.selectListByLambda(instanceWrapper);
        if (instances.isEmpty()) {
            return response;
        }

        // 设置审批状态并校验当前审批节点。
        ApprovalInstance latestInstance = instances.getFirst();
        response.setApproveStatus(latestInstance.getApprovalStatus());
        if (StringUtils.isBlank(latestInstance.getCurrentNodeId())) {
            return response;
        }

        // 只查询当前实例当前节点对应的审批任务。
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getInstanceId, latestInstance.getId())
                .eq(ApprovalTask::getNodeId, latestInstance.getCurrentNodeId());
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        if (tasks.isEmpty()) {
            return response;
        }

        // 批量加载审批人基础信息。
        List<String> approverIds = tasks.stream()
                .map(ApprovalTask::getApproverId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, User> userMap = approverIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByIds(approverIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (prev, next) -> prev));

        // 批量加载任务对应审批记录（用于审批意见）。
        List<String> taskIds = tasks.stream()
                .map(ApprovalTask::getId)
                .filter(StringUtils::isNotBlank)
                .toList();
        Map<String, ApprovalRecord> taskRecordMap = Collections.emptyMap();
        if (!taskIds.isEmpty()) {
            LambdaQueryWrapper<ApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.in(ApprovalRecord::getTaskId, taskIds)
                    .orderByAsc(ApprovalRecord::getCreateTime);
            List<ApprovalRecord> records = approvalRecordMapper.selectListByLambda(recordWrapper);
            taskRecordMap = records.stream()
                    .filter(record -> StringUtils.isNotBlank(record.getTaskId()))
                    .collect(Collectors.toMap(ApprovalRecord::getTaskId, Function.identity(), (prev, next) -> next));
        }

        // 组装审核人明细并按审批人去重返回。
        Map<String, UserApprovalDTO> approveUserMap = new LinkedHashMap<>();
        for (ApprovalTask task : tasks) {
            String approverId = task.getApproverId();
            if (StringUtils.isBlank(approverId)) {
                continue;
            }
            UserApprovalDTO userApprove = new UserApprovalDTO();
            userApprove.setId(approverId);
            User user = userMap.get(approverId);
            if (user != null) {
                userApprove.setName(user.getName());
                userApprove.setEmail(user.getEmail());
            }

            userApprove.setApproveResult(StringUtils.defaultIfBlank(task.getStatus(), ApprovalStatus.PENDING.name()));
            ApprovalRecord record = taskRecordMap.get(task.getId());
            if (record != null) {
                userApprove.setApproveReason(record.getComment());
            }
            approveUserMap.put(approverId, userApprove);
        }

        // 回填最终审核人列表。
        response.setApproveUserList(new ArrayList<>(approveUserMap.values()));
        return response;
    }

    /**
     * 更新业务表的审批状态
     *
     * @param formKey        表单类型
     * @param resourceId     资源ID
     * @param approvalStatus 审批状态
     */
    public void updateApprovalStatus(FormKey formKey, String resourceId, String approvalStatus) {
        String tableName = FORM_APPROVAL_TABLE.get(formKey.getKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        extApprovalInstanceMapper.updateApprovalStatus(tableName, resourceId, approvalStatus);
    }

    /**
     * 查询对应业务表的业务名
     *
     * @param formKey    表单类型
     * @param resourceId 资源ID
     */
    public String selectBusinessName(FormKey formKey, String resourceId) {
        String tableName = FORM_APPROVAL_TABLE.get(formKey.getKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        return extApprovalInstanceMapper.selectBusinessName(tableName, resourceId);
    }


    /**
     * 手动提审
     *
     * @param param 提审参数
     */
    public void push(ApprovalResourceBaseParam param, String currentOrgId, String currentUserId) {
        // 更新业务表 approval_status
        String tableName = FORM_APPROVAL_TABLE.get(param.getFormKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        extApprovalInstanceMapper.updateApprovalStatus(tableName, param.getResourceId(), ApprovalStatus.APPROVING.name());
        ApprovalFlowVersion approvalFlowVersion = approvalFlowService.getEnabledFlow(param.getFormKey(), currentOrgId);
        if (approvalFlowVersion == null) {
            throw new GenericException(Translator.get("approval_flow.not.exist"));
        }
        // 插入审批实例和第一个审批节点待办任务
        //提前创建实例id
        String instanceId = IDGenerator.nextStr();
        List<BaseModuleFieldValue> fvs = formService.compressResourceDetail(param.getFormKey(), param.getResourceId());
        //获取第一个节点
        ApprovalNodeApproverResponse firstApproverNode = approvalFlowService.getResourceApprovalFlowFirstApproverNode(approvalFlowVersion.getId(), fvs);
        //节点逻辑处理,返回最终需要操作的节点
        String resourceOwner = extApprovalInstanceMapper.getResourceOwner(tableName, param.getResourceId());
        ApprovalExceptionService approvalExceptionService = CommonBeanFactory.getBean(ApprovalExceptionService.class);
        ApprovalNodeApproverResponse finalNode = approvalExceptionService.nodeHandleAndSaveTask(firstApproverNode, fvs, instanceId, currentUserId, currentOrgId,
                param, approvalFlowVersion.getId(), resourceOwner);


    }

    public void revoke(ApprovalResourceRevokeParam param, String currentUserId) {
        // 更新业务表 approval_status
        String tableName = FORM_APPROVAL_TABLE.get(param.getFormKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        extApprovalInstanceMapper.updateApprovalStatus(tableName, param.getResourceId(), ApprovalStatus.REVOKED.name());
        // 更新审批实例状态
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(param.getInstanceId());
        instance.setApprovalStatus(ApprovalStatus.REVOKED.name());
        instance.setApprovalTime(System.currentTimeMillis());
        instance.setUpdateUser(currentUserId);
        instance.setUpdateTime(System.currentTimeMillis());
        approvalInstanceMapper.updateById(instance);
    }
}
