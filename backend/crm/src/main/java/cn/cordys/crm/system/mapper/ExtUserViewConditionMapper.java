package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.UserViewCondition;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserViewConditionMapper {


    List<UserViewCondition> getViewConditions(@Param("viewId") String viewId);

    void delete(@Param("viewId") String viewId);

    //根据userIds删除视图条件
    void deleteByUserIds(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);
}
