package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.request.AnnouncementPageRequest;
import cn.cordys.crm.system.dto.response.AnnouncementDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExtAnnouncementMapper {
    List<AnnouncementDTO> selectByBaseRequest(@Param("request") AnnouncementPageRequest request);

    AnnouncementDTO selectById(@Param("id") String id);

    List<AnnouncementDTO> selectInEffectUnConvertData(@Param("timestamp") long timestamp);

    List<String> selectFixTimeExpiredIds(@Param("startTime") long startTime, @Param("endTime") long endTime);

    int updateNotice(@Param("ids") List<String> ids, @Param("notice") Boolean notice, @Param("organizationId") String organizationId);

}
