package io.cordys.crm.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.cordys.crm.system.domain.MessageTaskBlob;

public interface ExtMessageTaskBlobMapper {
    List<MessageTaskBlob> getExtMessageTaskBlobByMessageTaskIds(@Param("messageTaskIds") List<String> messageTaskIds);
}
