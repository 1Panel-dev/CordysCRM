package cn.cordys.crm.search.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserSearchConfigMapper {

    //根据userIds删除用户搜索配置
    void deleteByUserIds(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);

}
