package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.response.UserViewListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserViewMapper {
    Long getNextPos(@Param("orgId") String orgId, @Param("userId") String userId, @Param("resourceType") String resourceType);

    int countUserView(@Param("userId") String userId, @Param("id") String id, @Param("orgId") String orgId);

    List<UserViewListResponse> selectViewList(@Param("resourceType") String resourceType, @Param("userId") String userId, @Param("orgId") String orgId);

    Long getPrePos(@Param("orgId") String orgId, @Param("basePos") Long basePos, @Param("userId") String userId, @Param("resourceType") String resourceType);

    Long getLastPos(@Param("orgId") String orgId, @Param("basePos") Long basePos, @Param("userId") String userId, @Param("resourceType") String resourceType);

    //根据userIds删除视图
    void deleteUserViewByUserIds(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);
}
