package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.Schedule;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtScheduleMapper {

  List<Schedule> getScheduleByLimit(@Param("start") int start, @Param("limit") int limit);
}
