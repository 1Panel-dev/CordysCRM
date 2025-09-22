package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.Navigation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtNavigationMapper {

    List<Navigation> selectList(@Param("orgId") String orgId);


    void moveUpNavigation(@Param("start") Long start, @Param("end") Long end);

    void moveDownNavigation(@Param("start") Long start, @Param("end") Long end);
}
