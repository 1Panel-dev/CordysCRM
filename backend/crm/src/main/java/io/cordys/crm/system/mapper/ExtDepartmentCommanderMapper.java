package io.cordys.crm.system.mapper;


import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtDepartmentCommanderMapper {

    void deleteByDepartmentIds(@Param("departmentIds") List<String> departmentIds);

    List<String> selectCommander(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);
}
