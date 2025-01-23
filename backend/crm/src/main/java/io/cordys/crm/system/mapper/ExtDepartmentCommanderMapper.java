package io.cordys.crm.system.mapper;


import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtDepartmentCommanderMapper {

    void deleteByDepartmentIds(@Param("departmentIds") List<String> departmentIds);
}
