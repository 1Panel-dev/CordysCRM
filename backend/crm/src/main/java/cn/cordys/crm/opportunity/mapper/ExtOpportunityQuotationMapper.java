package cn.cordys.crm.opportunity.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtOpportunityQuotationMapper {

  List<OpportunityQuotationListResponse> list(
      @Param("request") OpportunityQuotationPageRequest request,
      @Param("orgId") String orgId,
      @Param("userId") String userId,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission,
      @Param("source") boolean source);

  void updateApprovalStatus(
      @Param("approvingId") String approvingId,
      @Param("approvalStatus") String approvalStatus,
      @Param("userId") String userId,
      @Param("updateTime") long updateTime);

  // 根据时间戳获取报价单列表
  List<OpportunityQuotation> getQuotationByTimestamp(
      @Param("timestamp") long timestamp,
      @Param("timestampOld") long timestampOld,
      @Param("orgId") String orgId);
}
