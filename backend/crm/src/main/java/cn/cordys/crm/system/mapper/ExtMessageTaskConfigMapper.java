package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.MessageTaskConfig;
import org.apache.ibatis.annotations.Param;

public interface ExtMessageTaskConfigMapper {
  MessageTaskConfig getConfigByModuleAndEvent(
      @Param("module") String taskType,
      @Param("event") String event,
      @Param("organizationId") String organizationId);
}
