package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtExportTaskMapper {

  int getExportTaskCount(@Param("userId") String userId, @Param("status") String status);

  int updateExportTaskStatus(
      @Param("newStatus") String newStatus, @Param("oldStatus") String oldStatus);

  List<ExportTask> selectExportTaskList(
      @Param("request") ExportTaskCenterQueryRequest request,
      @Param("oneDayBefore") long oneDayBefore,
      @Param("userId") String userId);
}
