package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.dto.request.ApprovalFlowPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalFlowListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批流扩展Mapper
 */
public interface ExtApprovalFlowMapper {

    /**
     * 分页查询审批流列表（带用户名称）
     */
    List<ApprovalFlowListResponse> list(
            @Param("request") ApprovalFlowPageRequest request,
            @Param("organizationId") String organizationId);
}