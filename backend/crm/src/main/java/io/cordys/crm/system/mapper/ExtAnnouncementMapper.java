package io.cordys.crm.system.mapper;

import java.util.List;

import io.cordys.crm.system.dto.request.AnnouncementPageRequest;
import org.springframework.data.repository.query.Param;

import io.cordys.crm.system.dto.response.AnnouncementDTO;

public interface ExtAnnouncementMapper {
    List<AnnouncementDTO> selectByBaseRequest(@Param("request") AnnouncementPageRequest request);

    AnnouncementDTO selectById(@Param("id") String id);

    List<AnnouncementDTO> selectInEffectUnConvertData(@Param("timestamp") long timestamp);

    int updateNotice(@Param("ids") List<String> ids, @Param("notice") Boolean notice, @Param("organizationId") String organizationId);

}
