package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalAddSignRequest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalAddSignService extends ApprovalOperationBaseService {

    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalAddSignTask> approvalAddSignTasMapper;

    /**
     * 加签流程
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    public void addSign(ApprovalAddSignRequest request, String userId, String orgId) {
        //校验流程是否允许加签
        ApprovalFlowVersion approvalFlow = getFlowPermission(request.getInstanceId());
        if (approvalFlow == null || !approvalFlow.getAllowAddSign()) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
        //新增加签task
        addApprovalTask(request, userId, ApprovalTaskType.SN.name());
        //新增加签task拓展信息
        ApprovalAddSignTask approvalAddSignTask = addTaskInfo(request, userId);
        //保存加签附件
        addAttachment(request.getAttachmentIds(), request.getInstanceId(), userId, orgId, approvalAddSignTask.getId());
        //更新原task
        updateOrgTask(request);
        //todo if(request.type==after)  调用执行接口。

    }


    /**
     * 新增加签task拓展信息
     *
     * @param request
     */
    public ApprovalAddSignTask addTaskInfo(ApprovalAddSignRequest request, String userId) {
        ApprovalAddSignTask approvalAddSignTask = new ApprovalAddSignTask();
        approvalAddSignTask.setId(IDGenerator.nextStr());
        approvalAddSignTask.setSignTaskId(request.getId());

        //获取taskId
        ApprovalAddSignTask addSignTask = approvalAddSignTasMapper.selectListByLambda(
                new LambdaQueryWrapper<ApprovalAddSignTask>()
                        .eq(ApprovalAddSignTask::getSignTaskId, request.getId())
        ).stream().findFirst().orElse(null);
        if (addSignTask == null) {
            approvalAddSignTask.setTaskId(request.getId());
        } else {
            approvalAddSignTask.setTaskId(addSignTask.getTaskId());
        }

        approvalAddSignTask.setType(request.getType());
        approvalAddSignTask.setComment(request.getComment());
        approvalAddSignTasMapper.insert(approvalAddSignTask);
        return approvalAddSignTask;
    }

    /**
     * 更新原task
     *
     * @param request
     */
    private void updateOrgTask(ApprovalAddSignRequest request) {
        //更新原task action
        ApprovalTask approvalTask = approvalTaskMapper.selectByPrimaryKey(request.getId());
        if (Strings.CI.equals(request.getType(), ApprovalAddSignType.BEFORE.name())) {
            //之前：加签
            approvalTask.setAction(ApprovalAction.SIGN.name());
        }
        if (Strings.CI.equals(request.getType(), ApprovalAddSignType.AFTER.name())) {
            //之后：同意
            approvalTask.setAction(ApprovalAction.APPROVE.name());
        }
        approvalTaskMapper.update(approvalTask);
    }


}
