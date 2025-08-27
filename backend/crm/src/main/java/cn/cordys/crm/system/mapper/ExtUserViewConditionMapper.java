package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.UserViewCondition;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserViewConditionMapper {


    List<UserViewCondition> getViewConditions(@Param("viewId") String viewId);

    void delete(@Param("viewId") String viewId);
}
