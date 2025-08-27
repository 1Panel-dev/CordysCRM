package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.Schedule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtScheduleMapper {

    List<Schedule> getScheduleByLimit(@Param("start") int start, @Param("limit") int limit);
}
