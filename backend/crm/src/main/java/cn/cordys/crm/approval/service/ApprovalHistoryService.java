package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 按业务资源查询由远至近的历史审批详情。
 */
@Service
@Transactional(readOnly = true)
public class ApprovalHistoryService {

    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private ApprovalInstanceService approvalInstanceService;
    @Resource
    private ApprovalCommentService approvalCommentService;

    public List<ApprovalInstanceDetail> getHistoryInstanceDetail(String resourceId, String userId, String orgId) {
        return extApprovalInstanceMapper.selectInstancesByResourceId(resourceId, orgId)
                .stream().skip(1).filter(this::isCompleted)
                .filter(instance -> approvalResourceService.hasInstanceViewPermission(instance, userId, orgId))
                .map(instance -> {
                    ApprovalInstanceDetail detail = approvalInstanceService.getHistoricalApprovalInstanceDetail(instance,
                            orgId);
                    detail.setCommentable(approvalCommentService.hasCommentPermission(resourceId, userId, orgId));
                    return detail;
                }).toList().reversed();
    }

    private boolean isCompleted(ApprovalInstance instance) {
        return ApprovalStatus.APPROVED.name().equals(instance.getApprovalStatus())
                || ApprovalStatus.UNAPPROVED.name().equals(instance.getApprovalStatus());
    }
}
