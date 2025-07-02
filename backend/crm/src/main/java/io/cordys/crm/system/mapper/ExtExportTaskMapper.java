package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;
public interface ExtExportTaskMapper {

    int getExportTaskCount(@Param("userId")String userId, @Param("resourceType")String resourceType, @Param("status")String status);

    int updateExportTaskStatus(@Param("newStatus")String newStatus, @Param("oldStatus")String oldStatus);
}
