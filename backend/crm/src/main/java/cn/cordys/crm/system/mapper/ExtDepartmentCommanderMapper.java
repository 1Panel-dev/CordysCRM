package cn.cordys.crm.system.mapper;


import cn.cordys.crm.system.domain.DepartmentCommander;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtDepartmentCommanderMapper {

    void deleteByDepartmentIds(@Param("departmentIds") List<String> departmentIds);

    List<String> selectCommander(@Param("departmentId") String departmentId, @Param("orgId") String orgId);

    List<DepartmentCommander> selectCommanderByUsers(@Param("departmentId") String departmentId, @Param("userIds") List<String> userIds);
}
