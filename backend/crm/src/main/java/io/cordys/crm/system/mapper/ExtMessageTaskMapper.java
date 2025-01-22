package io.cordys.crm.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.cordys.crm.system.domain.MessageTask;

public interface ExtMessageTaskMapper {

   List<MessageTask> getEnableMessageTaskByReceiveTypeAndTaskType(@Param("receiveType") String receiveType, @Param("module") String taskType, @Param("organizationId") String organizationId);

   List<MessageTask> getMessageTaskByReceiveTypeAndTaskType(@Param("receiveType") String receiveType, @Param("module") String taskType, @Param("organizationId") String organizationId);

   List<MessageTask>getMessageTaskList(@Param("organizationId") String organizationId);
}
