package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.system.domain.User;
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
public class ResourceApprovalService {

    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;
    @Resource
    private BaseMapper<User> userMapper;

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

            userApprove.setApproveResult(StringUtils.defaultIfBlank(task.getTaskStatus(), ApprovalStatus.PENDING.name()));
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
}
