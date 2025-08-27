package cn.cordys.crm.follow.mapper;

import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFollowUpRecordMapper {

    List<FollowUpRecordListResponse> selectPoolList(@Param("request") FollowUpRecordPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId, @Param("resourceType") String resourceType, @Param("type") String type);


    List<FollowUpRecordListResponse> selectList(@Param("request") FollowUpRecordPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                                                @Param("resourceType") String resourceType, @Param("type") String type, @Param("customerData") CustomerDataDTO customerData);

    FollowUpRecord selectRecord(@Param("customerId") String customerId, @Param("opportunityId") String opportunityId, @Param("clueId") String clueId, @Param("orgId") String orgId, @Param("type") String type);

    Long getNewContactCount(@Param("request") HomeStatisticSearchWrapperRequest request);
}
