package cn.cordys.crm.follow.mapper;

import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFollowUpPlanMapper {


    List<FollowUpPlanListResponse> selectList(@Param("request") FollowUpPlanPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                                              @Param("resourceType") String resourceType, @Param("type") String type, @Param("customerData") CustomerDataDTO customerData,
                                              @Param("resourceTypeList") List<String> resourceTypeList);

    List<FollowUpPlan> selectPlanByTimestamp(@Param("timestamp") long timestamp);

    Long getNewFollowUpPlan(@Param("request") HomeStatisticSearchWrapperRequest request);
}
