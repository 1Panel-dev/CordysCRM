package cn.cordys.crm.search.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtUserSearchConfigMapper {

  // 根据userIds删除用户搜索配置
  void deleteByUserIds(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);
}
