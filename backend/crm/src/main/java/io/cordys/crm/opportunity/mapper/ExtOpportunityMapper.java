package io.cordys.crm.opportunity.mapper;


import io.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityTransferRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtOpportunityMapper {


    List<OpportunityListResponse> list(@Param("request") OpportunityPageRequest request, @Param("orgId") String orgId);

    List<String> selectByProducts(@Param("request") OpportunityAddRequest request, @Param("orgId") String orgId, @Param("id") String id);

    void batchTransfer(@Param("request") OpportunityTransferRequest request);
}
