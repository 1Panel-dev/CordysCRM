package io.cordys.crm.follow.mapper;

import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFollowUpRecordMapper {

    List<FollowUpRecordListResponse> selectList(@Param("request") FollowUpRecordPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId, @Param("resourceType") String resourceType, @Param("type") String type);
}
