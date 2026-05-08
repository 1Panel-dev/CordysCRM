package cn.cordys.crm.approval.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalReturnBackRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalReturnBackService extends ApprovalOperationBaseService {

    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalReturnBackRecord> approvalReturnBackRecordMapper;

    /**
     * 回退操作
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public void back(ApprovalReturnBackRequest request, String userId, String orgId) {
        //新增退回task
        addApprovalTask(request, userId, ApprovalTaskType.BK.name());
        //新增退回拓展信息
        ApprovalReturnBackRecord approvalReturnBackRecord = addTaskInfo(request, userId);
        //保存退回附件
        addAttachment(request.getAttachmentIds(), request.getInstanceId(), userId, orgId, approvalReturnBackRecord.getId());
        //更新原task
        updateOrgTask(request);
    }




    /**
     * 新增退回拓展信息
     *
     * @param request
     * @param userId
     * @return
     */
    public ApprovalReturnBackRecord addTaskInfo(ApprovalReturnBackRequest request, String userId) {
        ApprovalReturnBackRecord returnBack = new ApprovalReturnBackRecord();
        returnBack.setId(IDGenerator.nextStr());
        returnBack.setTaskId(request.getId());
        returnBack.setReturnToTaskId(request.getReturnToTaskId());
        returnBack.setReturnReason(request.getReturnReason());
        returnBack.setReturnUserId(userId);
        approvalReturnBackRecordMapper.insert(returnBack);
        return returnBack;
    }


    /**
     * 更新原task
     *
     * @param request
     */
    private void updateOrgTask(ApprovalReturnBackRequest request) {
        //更新原task action
        ApprovalTask approvalTask = approvalTaskMapper.selectByPrimaryKey(request.getId());
        approvalTask.setAction(ApprovalAction.BACK.name());
        approvalTaskMapper.update(approvalTask);
    }
}
