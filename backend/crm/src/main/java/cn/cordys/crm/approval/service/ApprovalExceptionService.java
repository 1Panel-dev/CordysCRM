package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * 审批流程异常处理服务
 */
@Service
public class ApprovalExceptionService {

    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;


    public ApprovalNodeApproverResponse nodeHandleAndSaveTask(ApprovalNodeApproverResponse node, List<BaseModuleFieldValue> fieldValues, String instanceId, String userId, String orgId,
                                                              ApprovalResourceBaseParam param, String flowVersionId, String resourceOwner) {
        ApprovalNodeApproverResponse finalNode = nodeHandle(node, fieldValues, instanceId, userId, orgId, param, flowVersionId);
        //创建审批实例 和审批任务
        saveInstanceAndApprovalTask(finalNode, resourceOwner, instanceId, userId, orgId, param, flowVersionId, fieldValues);

        return finalNode;
    }

    /**
     * 创建审批实例 和审批任务
     *
     * @param finalNode
     * @param resourceOwner
     * @param instanceId
     * @param userId
     * @param orgId
     * @param param
     * @param flowVersionId
     * @param fieldValues
     */
    private void saveInstanceAndApprovalTask(ApprovalNodeApproverResponse finalNode, String resourceOwner, String instanceId, String userId,
                                             String orgId, ApprovalResourceBaseParam param, String flowVersionId, List<BaseModuleFieldValue> fieldValues) {
        if (finalNode != null && Strings.CI.equals(finalNode.getNodeType(), ApprovalNodeTypeEnum.APPROVER.name())) {
            //审批类型 1创建审批实例
            addApprovalInstance(instanceId, finalNode.getId(), param, flowVersionId, userId, ApprovalStatus.APPROVING.name());
            saveNodeTasks(resourceOwner, finalNode, instanceId, userId, orgId, null, fieldValues, param, flowVersionId);
        }

    }

    /**
     * 保存审批节点待办任务
     *
     * @param resourceOwner
     * @param finalNode
     * @param instanceId
     * @param userId
     * @param orgId
     * @param taskType
     * @param fieldValues
     * @param param
     * @param flowVersionId
     */
    public void saveNodeTasks(String resourceOwner, ApprovalNodeApproverResponse finalNode, String instanceId, String userId, String orgId,
                              String taskType, List<BaseModuleFieldValue> fieldValues, ApprovalResourceBaseParam param, String flowVersionId) {
        List<ApprovalTask> approvalTasks = new ArrayList<>();
        ApprovalNodeApprover approvalNodeApprover = approvalNodeApproverMapper.selectByPrimaryKey(finalNode.getId());
        List<User> approvers = approvalFlowService.getCurrentNodeMultiApprover(finalNode.getId(), userId, orgId);
        if (Strings.CI.equals(approvalNodeApprover.getMultiApproverMode(), MultiApproverModeEnum.SEQUENTIAL.name()) || approvers.size() == 1) {
            // 单人或者依次审批, 只会产生一条待办任务
            User approverUser = approvers.getFirst();
            //异常处理逻辑
            exceptionHandle(approverUser.getId(), resourceOwner, finalNode, instanceId, ApprovalTaskType.NL.name(), userId, fieldValues, orgId, param, flowVersionId);
        } else {
            // 多人审批, 且为会签或签方式
            approvers.forEach(approver -> {
                //todo 多人审批任务的异常处理
                ApprovalTask approvalTask = buildTask(finalNode.getId(), instanceId, approver.getId(), taskType, userId, null);
                approvalTasks.add(approvalTask);
            });
        }
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(approvalTasks)) {
            approvalTaskMapper.batchInsert(approvalTasks);
        }
    }

    /**
     * 审批人与提交人为同一人异常处理
     *
     * @param approverUserId
     * @param resourceOwner
     * @param finalNode
     * @param instanceId
     * @param taskType
     * @param userId
     * @param fieldValues
     * @param orgId
     * @param param
     * @param flowVersionId
     */
    private void exceptionHandle(String approverUserId, String resourceOwner, ApprovalNodeApproverResponse finalNode, String instanceId, String taskType,
                                 String userId, List<BaseModuleFieldValue> fieldValues, String orgId, ApprovalResourceBaseParam param, String flowVersionId) {
        if (Strings.CI.equals(approverUserId, resourceOwner)) {
            SameSubmitterActionEnum submitterActionEnum = EnumUtils.valueOf(SameSubmitterActionEnum.class, finalNode.getEmptyApproverAction());
            switch (submitterActionEnum) {
                case SKIP -> {
                    //自动跳过：
                    //1.新增同意ask
                    ApprovalTask approvalTask = buildTask(finalNode.getId(), instanceId, approverUserId, taskType, userId, ApprovalStatus.APPROVED.name());
                    approvalTaskMapper.insert(approvalTask);
                    //获取下一个node节点
                    ApprovalNodeResponse nextNode = approvalFlowService.getNextNode(finalNode.getId(), fieldValues);
                    nodeHandleAndSaveTask((ApprovalNodeApproverResponse) nextNode, fieldValues, instanceId, userId, orgId, param, flowVersionId, resourceOwner);
                }
                case ALLOW -> {
                    //由提交人审批
                    //1.创建提交人task
                    ApprovalTask approvalTask = buildTask(finalNode.getId(), instanceId, approverUserId, taskType, userId, ApprovalStatus.APPROVED.name());
                    approvalTaskMapper.insert(approvalTask);
                }
                case ASSIGN_SUPERIOR -> {
                    LambdaQueryWrapper<OrganizationUser> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(OrganizationUser::getUserId, userId);
                    wrapper.eq(OrganizationUser::getOrganizationId, orgId);
                    List<OrganizationUser> organizationUsers = organizationUserMapper.selectListByLambda(wrapper);
                    if (CollectionUtils.isNotEmpty(organizationUsers) && StringUtils.isNotBlank(organizationUsers.getFirst().getSupervisorId())) {
                        String supervisorId = organizationUsers.getFirst().getSupervisorId();
                        ApprovalTask approvalTask = buildTask(finalNode.getId(), instanceId, approverUserId, taskType, supervisorId, ApprovalStatus.APPROVED.name());
                        approvalTaskMapper.insert(approvalTask);
                    }
                }
            }
        }
    }

    /**
     * 构建task
     *
     * @param nodeId
     * @param instanceId
     * @param approverId
     * @param taskType
     * @param currentUserId
     * @param status
     * @return
     */
    private ApprovalTask buildTask(String nodeId, String instanceId, String approverId, String taskType, String currentUserId, String status) {
        ApprovalTask approvalTask = new ApprovalTask();
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setNodeId(nodeId);
        approvalTask.setInstanceId(instanceId);
        approvalTask.setApproverId(approverId);
        approvalTask.setStatus(status);
        approvalTask.setType(StringUtils.isBlank(taskType) ? ApprovalTaskType.NL.name() : taskType);
        approvalTask.setCreateTime(System.currentTimeMillis());
        approvalTask.setUpdateTime(System.currentTimeMillis());
        approvalTask.setCreateUser(currentUserId);
        approvalTask.setUpdateUser(currentUserId);
        return approvalTask;
    }

    /**
     * 提审 节点逻辑处理
     *
     * @param node
     * @param fieldValues
     * @param instanceId
     * @param userId
     * @param orgId
     * @param param
     * @param flowVersionId
     * @return
     */
    public ApprovalNodeApproverResponse nodeHandle(ApprovalNodeApproverResponse node, List<BaseModuleFieldValue> fieldValues, String instanceId, String userId, String orgId,
                                                   ApprovalResourceBaseParam param, String flowVersionId) {
        //如果当前节点已经是end节点了，不处理
        if (Strings.CI.equals(node.getNodeType(), ApprovalNodeTypeEnum.END.name())) {
            return null;
        }

        //如果当前节点是审批节点，校验异常处理逻辑
        if (Strings.CI.equals(node.getNodeType(), ApprovalNodeTypeEnum.APPROVER.name())) {
            if (node instanceof ApprovalNodeApproverResponse nodeApprover) {
                // if(审批类型 == 自动通过): 1.新增审批记录 2.继续获取下一个node节点
                if (Strings.CI.equals(nodeApprover.getApprovalType(), ApprovalTypeEnum.AUTO_PASS.name())) {
                    ApprovalNodeApproverResponse nextNode = addRecordAndGetNextNode(instanceId, nodeApprover.getId(), fieldValues, userId, "自动通过");
                    return nodeHandle(nextNode, fieldValues, instanceId, userId, orgId, param, flowVersionId);

                }

                // if(审批类型 == 自动拒绝)：1.新增审批记录 2.流程直接终止并标记为拒绝
                if (Strings.CI.equals(nodeApprover.getApprovalType(), ApprovalTypeEnum.AUTO_REJECT.name())) {
                    addRecordAndRejectApproval(instanceId, nodeApprover.getId(), userId, "自动拒绝", param, flowVersionId);
                    return null;
                }

                //审批类型 == 人工审批 1.处理异常逻辑
                //获取当前节点的审批人
                List<User> approvers = approvalFlowService.getCurrentNodeMultiApprover(nodeApprover.getId(), userId, orgId);
                //如果审批人是空异常逻辑处理
                if (CollectionUtils.isEmpty(approvers)) {
                    //自动通过/指定人员处理/转交给审批管理员
                    EmptyApproverActionEnum emptyApproverActionEnum = EnumUtils.valueOf(EmptyApproverActionEnum.class, nodeApprover.getEmptyApproverAction());
                    switch (emptyApproverActionEnum) {
                        case AUTO_PASS -> {
                            //自动通过：1.新增审批记录 2.获取下一个node节点
                            ApprovalNodeApproverResponse nextNode = addRecordAndGetNextNode(instanceId, nodeApprover.getId(), fieldValues, userId, "审批人为空，自动通过");
                            return nodeHandle(nextNode, fieldValues, instanceId, userId, orgId, param, flowVersionId);
                        }
                        case ASSIGN_SPECIFIC -> {
                            //指定人员处理： 返回人员列表
                            nodeApprover.setApproverType(ApproverTypeEnum.MEMBER.name());
                            nodeApprover.setApproverList(List.of(nodeApprover.getFallbackApprover()));
                            return nodeApprover;
                        }
                        case ASSIGN_ADMIN -> {
                            //审批管理员：返回人员
                            nodeApprover.setApproverType(ApproverTypeEnum.MEMBER.name());
                            nodeApprover.setApproverList(List.of(nodeApprover.getFallbackApprover()));
                            return nodeApprover;
                        }
                        default -> {
                            return nodeApprover;
                        }
                    }
                }
                //审批人与提交人为同一人异常处理(创建任务的时候处理)saveInstanceAndApprovalTask方法处理
            }
        }
        return node;
    }


    /**
     * 自动拒绝
     * 1.新增审批记录
     * 2.流程直接终止并标记为拒绝
     *
     * @param instanceId
     * @param nodeId
     * @param userId
     * @param comment
     * @param param
     * @param flowVersionId
     */
    private void addRecordAndRejectApproval(String instanceId, String nodeId, String userId, String comment, ApprovalResourceBaseParam param, String flowVersionId) {
        //新增审批记录
        addRecord(instanceId, nodeId, ApprovalStatus.UNAPPROVED.name(), comment, userId);
        //终止流程
        //1.创建审批实例 状态标记为拒绝
        addApprovalInstance(instanceId, nodeId, param, flowVersionId, userId, ApprovalStatus.UNAPPROVED.name());
        //2.更新业务表状态
        approvalResourceService.updateApprovalStatus(FormKey.valueOf(param.getFormKey()), param.getResourceId(), ApprovalStatus.UNAPPROVED.name());
    }


    /**
     * 新增审批实例
     *
     * @param instanceId
     * @param nodeId
     * @param param
     * @param flowVersionId
     * @param userId
     * @param approvalStatus
     */
    private void addApprovalInstance(String instanceId, String nodeId, ApprovalResourceBaseParam param, String flowVersionId, String userId, String approvalStatus) {
        ApprovalInstance approvalInstance = new ApprovalInstance();
        approvalInstance.setId(instanceId);
        if (approvalInstanceMapper.countByExample(approvalInstance) == 0) {
            approvalInstance.setFlowVersionId(flowVersionId);
            approvalInstance.setType(param.getFormKey());
            approvalInstance.setResourceId(param.getResourceId());
            approvalInstance.setSubmitterId(userId);
            approvalInstance.setSubmitTime(System.currentTimeMillis());
            approvalInstance.setApprovalStatus(approvalStatus);
            approvalInstance.setCurrentNodeId(nodeId);
            approvalInstance.setCreateTime(System.currentTimeMillis());
            approvalInstance.setCreateUser(userId);
            approvalInstance.setUpdateTime(System.currentTimeMillis());
            approvalInstance.setUpdateUser(userId);
            approvalInstanceMapper.insert(approvalInstance);
        }
    }


    /**
     * 自动通过
     * 1.新增审批记录
     * 2.获取下一个node节点
     *
     * @param instanceId
     * @param nodeId
     * @param fieldValues
     * @param userId
     * @param comment
     * @return
     */
    private ApprovalNodeApproverResponse addRecordAndGetNextNode(String instanceId, String nodeId, List<BaseModuleFieldValue> fieldValues, String userId, String comment) {
        //新增审批记录
        addRecord(instanceId, nodeId, ApprovalStatus.APPROVED.name(), comment, userId);
        //todo 审批通过后的字段处理逻辑
        //通过后获取下一个节点
        ApprovalNodeResponse nextNode = approvalFlowService.getNextNode(nodeId, fieldValues);
        return (ApprovalNodeApproverResponse) nextNode;
    }


    /**
     * 新增审批记录
     *
     * @param instanceId
     * @param nodeId
     * @param name
     * @param comment
     * @param userId
     */
    private void addRecord(String instanceId, String nodeId, String name, String comment, String userId) {
        //新增审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId(IDGenerator.nextStr());
        record.setInstanceId(instanceId);
        record.setNodeId(nodeId);
        record.setResult(ApprovalStatus.APPROVED.name());
        record.setComment(comment);
        record.setCreateTime(System.currentTimeMillis());
        record.setCreateUser(userId);
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(userId);
        approvalRecordMapper.insert(record);
    }


}
