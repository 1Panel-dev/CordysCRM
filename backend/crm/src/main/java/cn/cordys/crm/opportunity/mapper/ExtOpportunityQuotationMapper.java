package cn.cordys.crm.opportunity.mapper;


import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtOpportunityQuotationMapper {

    List<OpportunityQuotationListResponse> list(@Param("request") OpportunityQuotationPageRequest request, @Param("orgId") String orgId,
                                                @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission, @Param("source") boolean source);

    void updateApprovalStatus(@Param("approvingId") String approvingId, @Param("approvalStatus") String approvalStatus, @Param("userId") String userId, @Param("updateTime") long updateTime);
}
