package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.common.exception.GenericException;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Announcement;
import io.cordys.crm.system.dto.request.AnnouncementPageRequest;
import io.cordys.crm.system.dto.request.AnnouncementRequest;
import io.cordys.crm.system.dto.response.AnnouncementDTO;
import io.cordys.crm.system.mapper.ExtAnnouncementMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AnnouncementService {

    @Resource
    private BaseMapper<Announcement> announcementMapper;
    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_ANNOUNCEMENT, type = LogType.ADD)
    public void add(AnnouncementRequest request, String userId) {
        Announcement announcement = new Announcement();
        announcement.setId(IDGenerator.nextStr());
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setReceiver(request.getReceiver());
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setCreateTime(System.currentTimeMillis());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setCreateUser(userId);
        announcement.setUpdateUser(userId);
        announcement.setStatus(NotificationConstants.Status.UNREAD.name());
        announcementMapper.insert(announcement);

        // 添加日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(announcement.getId())
                        .resourceName(announcement.getSubject())
                        .modifiedValue(announcement)
                        .build()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_ANNOUNCEMENT, type = LogType.UPDATE)
    public void update(AnnouncementRequest request, String userId) {
        Announcement originalAnnouncement = announcementMapper.selectByPrimaryKey(request.getId());
        if (originalAnnouncement == null) {
            throw new GenericException(Translator.get("announcement.blank"));
        }
        Announcement announcement = BeanUtils.copyBean(new Announcement(), request);
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setReceiver(request.getReceiver());
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setUpdateUser(userId);
        announcement.setStatus(NotificationConstants.Status.UNREAD.name());
        announcementMapper.updateById(announcement);

        // 添加日志上下文
        String resourceName = Optional.ofNullable(announcement.getSubject()).orElse(originalAnnouncement.getSubject());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originalAnnouncement)
                        .resourceId(originalAnnouncement.getId())
                        .resourceName(resourceName)
                        .modifiedValue(announcementMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_ANNOUNCEMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Announcement announcement = announcementMapper.selectByPrimaryKey(id);
        if (announcement == null) {
            throw new RuntimeException(Translator.get("announcement.blank"));
        }
        announcementMapper.deleteByPrimaryKey(id);

        // 添加日志上下文
        OperationLogContext.setResourceName(announcement.getSubject());
    }

    /**
     * 公告列表分页查询
     *
     * @param request
     * @return
     */
    public List<AnnouncementDTO> page(AnnouncementPageRequest request) {
        List<AnnouncementDTO> announcementDTOS = extAnnouncementMapper.selectByBaseRequest(request);
        if (CollectionUtils.isNotEmpty(announcementDTOS)) {
            for (AnnouncementDTO announcementDTO : announcementDTOS) {
                announcementDTO.setContentText(new String(announcementDTO.getContent()));
            }
        }
        return announcementDTOS;
    }

    /**
     * 获取公告
     *
     * @param id
     * @return
     */
    public AnnouncementDTO detail(String id) {
        AnnouncementDTO announcementDTO = extAnnouncementMapper.selectById(id);
        if (announcementDTO == null) {
            throw new RuntimeException(Translator.get("announcement.blank"));
        }
        announcementDTO.setContentText(new String(announcementDTO.getContent()));
        return announcementDTO;
    }
}

