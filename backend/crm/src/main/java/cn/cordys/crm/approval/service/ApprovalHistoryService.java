package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.response.ApprovalInstanceHistoryResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.cordys.crm.approval.service.ApprovalInstanceService.SIGN_SPILT;

/**
 * 按原审批实例查询历史轮次。
 */
@Service
@Transactional(readOnly = true)
public class ApprovalHistoryService {

    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalNode> approvalNodeMapper;
    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private ApprovalInstanceService approvalInstanceService;
    @Resource
    private ApprovalCommentService approvalCommentService;

    public List<ApprovalInstanceHistoryResponse> getHistoryInstances(String resourceId, String userId, String orgId) {
        List<ApprovalInstance> instances = extApprovalInstanceMapper.selectInstancesByResourceId(resourceId, orgId)
                .stream().skip(1).filter(this::isCompleted)
                .filter(instance -> approvalResourceService.hasInstanceViewPermission(instance, userId, orgId))
                .toList();
        List<String> nodeIds = instances.stream()
                .map(instance -> StringUtils.substringBefore(instance.getCurrentNodeId(), SIGN_SPILT))
                .filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, String> nodeNames = nodeIds.isEmpty() ? Map.of() : approvalNodeMapper.selectByIds(nodeIds).stream()
                .collect(Collectors.toMap(ApprovalNode::getId, ApprovalNode::getName));
        return instances.stream().map(instance -> {
            ApprovalInstanceHistoryResponse response = new ApprovalInstanceHistoryResponse();
            response.setId(instance.getId());
            response.setApprovalStatus(instance.getApprovalStatus());
            response.setCurrentNodeId(instance.getCurrentNodeId());
            response.setCurrentNodeName(nodeNames.get(StringUtils.substringBefore(
                    StringUtils.defaultString(instance.getCurrentNodeId()), SIGN_SPILT)));
            return response;
        }).toList();
    }

    public ApprovalInstanceDetail getHistoryInstanceDetail(String instanceId, String userId, String orgId) {
        ApprovalInstance instance = extApprovalInstanceMapper.selectInstanceById(instanceId, orgId);
        if (instance == null || !isCompleted(instance)) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }
        if (!approvalResourceService.hasInstanceViewPermission(instance, userId, orgId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        ApprovalInstanceDetail detail = approvalInstanceService.getHistoricalApprovalInstanceDetail(instance, orgId);
        detail.setCommentable(approvalCommentService.hasCommentPermission(instance.getId(), userId, orgId));
        return detail;
    }

    private boolean isCompleted(ApprovalInstance instance) {
        return ApprovalStatus.APPROVED.name().equals(instance.getApprovalStatus())
                || ApprovalStatus.UNAPPROVED.name().equals(instance.getApprovalStatus());
    }
}
