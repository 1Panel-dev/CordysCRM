package io.cordys.crm.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.cordys.crm.system.domain.MessageTask;

public interface ExtMessageTaskMapper {

    List<MessageTask> getEnableMessageTaskByReceiveTypeAndTaskType(@Param("module") String taskType, @Param("organizationId") String organizationId);

    List<MessageTask> getMessageTaskList(@Param("organizationId") String organizationId);

    MessageTask getMessageByModuleAndEvent(@Param("module") String taskType, @Param("event") String event, @Param("organizationId") String organizationId);

}
