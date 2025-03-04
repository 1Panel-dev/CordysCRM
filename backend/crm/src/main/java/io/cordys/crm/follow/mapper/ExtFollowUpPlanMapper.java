package io.cordys.crm.follow.mapper;

import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFollowUpPlanMapper {


    List<FollowUpPlanListResponse> selectList(@Param("request") FollowUpPlanPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId, @Param("resourceType") String resourceType, @Param("type") String type);
}
