package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.Navigation;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtNavigationMapper {

  List<Navigation> selectList(@Param("orgId") String orgId);

  void moveUpNavigation(@Param("start") Long start, @Param("end") Long end);

  void moveDownNavigation(@Param("start") Long start, @Param("end") Long end);
}
