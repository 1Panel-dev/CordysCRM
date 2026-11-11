package cn.cordys.crm.customer.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtCustomerCollaborationMapper {
    void updatePermission(@Param("id") String id, @Param("collaborationType") String collaborationType,
            @Param("expireTime") Long expireTime, @Param("updateUser") String updateUser,
            @Param("updateTime") long updateTime);
}
