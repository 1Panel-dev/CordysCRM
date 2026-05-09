package cn.cordys.crm.approval.service;


import cn.cordys.common.constants.FormKey;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalRejectRequest;
import cn.cordys.crm.system.domain.User;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalRejectService extends ApprovalOperationBaseService {

    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private ResourceApprovalService resourceApprovalService;
    @Resource
    private BaseMapper<User> userMapper;

    /**
     * 驳回操作
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public void reject(ApprovalRejectRequest request, String userId, String orgId) {
        //更新当前task action=REJECT
        updateTask(request);
        //更新 instance 和业务表 status
        ApprovalInstance approvalInstance = updateInstance(request.getInstanceId());
        updateBusinessStatus(approvalInstance);
        String resourceName = resourceApprovalService.selectBusinessName(FormKey.valueOf(approvalInstance.getType()), approvalInstance.getResourceId());
        User user = userMapper.selectByPrimaryKey(approvalInstance.getSubmitterId());
        sendNotice(approvalInstance, resourceName, orgId,
                Translator.getWithArgs("approval_reject", user.getName(), Translator.get(approvalInstance.getType()), resourceName));
    }


    /**
     * 更新业务表
     *
     * @param approvalInstance
     */
    private void updateBusinessStatus(ApprovalInstance approvalInstance) {
        resourceApprovalService.updateApprovalStatus(FormKey.valueOf(approvalInstance.getType()), approvalInstance.getResourceId(), ApprovalStatus.UNAPPROVED.name());
    }


    /**
     * 更新instance 状态
     *
     * @param id
     */
    private ApprovalInstance updateInstance(String id) {
        ApprovalInstance approvalInstance = approvalInstanceMapper.selectByPrimaryKey(id);
        approvalInstance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
        approvalInstance.setApprovalTime(System.currentTimeMillis());
        approvalInstanceMapper.update(approvalInstance);
        return approvalInstance;
    }


    /**
     * 更新task
     *
     * @param request
     */
    private void updateTask(ApprovalRejectRequest request) {
        ApprovalTask approvalTask = approvalTaskMapper.selectByPrimaryKey(request.getId());
        approvalTask.setAction(ApprovalAction.REJECT.name());
        approvalTaskMapper.update(approvalTask);
    }
}
