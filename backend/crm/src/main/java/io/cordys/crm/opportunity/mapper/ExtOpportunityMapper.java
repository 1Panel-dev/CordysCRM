package io.cordys.crm.opportunity.mapper;


import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionCountDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityTransferRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityRepeatResponse;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


public interface ExtOpportunityMapper {


    List<OpportunityListResponse> list(@Param("request") OpportunityPageRequest request, @Param("orgId") String orgId,
                                       @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<String> selectByProducts(@Param("request") OpportunityAddRequest request, @Param("orgId") String orgId, @Param("id") String id);

    void batchTransfer(@Param("request") OpportunityTransferRequest request);

    OpportunityDetailResponse getDetail(@Param("id") String id);

    List<OpportunityRepeatResponse> getRepeatList(@Param("customerId") String customerId);

    List<OptionDTO> getRepeatCountMap(@Param("customerIds") List<String> customerIds);

    int countByOwner(@Param("owner") String owner);

    List<OptionDTO> getOpportunityOptionsByIds(@Param("ids") List<String> ids);
}
