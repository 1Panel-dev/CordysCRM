package cn.cordys.crm.opportunity.mapper;


import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunitySearchStatisticRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityTransferRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunitySearchStatisticResponse;
import cn.cordys.crm.search.response.advanced.AdvancedOpportunityResponse;
import cn.cordys.crm.search.response.advanced.OpportunityRepeatResponse;
import cn.cordys.crm.search.response.global.GlobalOpportunityResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtOpportunityMapper {


    List<OpportunityListResponse> list(@Param("request") OpportunityPageRequest request, @Param("orgId") String orgId,
                                       @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    OpportunitySearchStatisticResponse searchStatistic(@Param("request") OpportunitySearchStatisticRequest request, @Param("orgId") String orgId,
                                                       @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<String> selectByProducts(@Param("request") OpportunityAddRequest request, @Param("orgId") String orgId, @Param("id") String id);

    void batchTransfer(@Param("request") OpportunityTransferRequest request, @Param("userId") String userId, @Param("updateTime") long updateTime);

    OpportunityDetailResponse getDetail(@Param("id") String id);

    List<OpportunityRepeatResponse> getRepeatList(@Param("customerId") String customerId);

    List<OptionDTO> getRepeatCountMap(@Param("customerIds") List<String> customerIds);

    int countByOwner(@Param("owner") String owner);

    List<OptionDTO> getOpportunityOptionsByIds(@Param("ids") List<String> ids);

    List<OptionDTO> getOpportunityOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);


    List<OpportunityListResponse> getListByIds(@Param("ids") List<String> ids);

    Long selectOpportunityCount(@Param("request") HomeStatisticSearchWrapperRequest request, @Param("amount") boolean amount, @Param("success") boolean success);

    List<AdvancedOpportunityResponse> advancedSearchList(@Param("request") OpportunityPageRequest request, @Param("orgId") String orgId);

    List<GlobalOpportunityResponse> globalSearchList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    long globalSearchListCount(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    List<Opportunity> searchColumnsByIds(@Param("columns") List<String> columns, @Param("ids") List<String> opportunityIds);

    /**
     * 全量更新商机
     *
     * @param opportunity 商机
     */
    void updateIncludeNullById(@Param("opportunity") Opportunity opportunity);

    void batchUpdate(@Param("request") BatchUpdateDbParam request);

    void moveUpOpportunity(@Param("start") Long start, @Param("end") Long end, @Param("stage") String stage);

    void moveDownOpportunity(@Param("start") Long start, @Param("end") Long end, @Param("stage") String stage);

    Long selectNextPos(@Param("orgId") String orgId, @Param("stage") String stage);

    void transfer(@Param("owner") String owner, @Param("userId") String userId, @Param("id") String id, @Param("updateTime") long updateTime, @Param("nextPos") long nextPos);

    void moveDownStageOpportunity(@Param("end")Long end, @Param("stage")String stage);
}
