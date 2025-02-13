package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.SubListUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Announcement;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.AnnouncementReceiveTypeDTO;
import io.cordys.crm.system.dto.request.AnnouncementPageRequest;
import io.cordys.crm.system.dto.request.AnnouncementRequest;
import io.cordys.crm.system.dto.response.AnnouncementDTO;
import io.cordys.crm.system.mapper.*;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AnnouncementService {

    @Resource
    private BaseMapper<Announcement> announcementMapper;
    @Resource
    private BaseMapper<Notification> notificationBaseMapper;
    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ExtRoleMapper extRoleMapper;
    @Resource
    private ExtNotificationMapper notificationMapper;
    @Resource
    private ExtUserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_ANNOUNCEMENT, type = LogType.ADD)
    public void add(AnnouncementRequest request, String userId) {
        Set<String> userSet = new HashSet<>();
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO = new AnnouncementReceiveTypeDTO();
        if (CollectionUtils.isNotEmpty(request.getRoleIds())) {
            userSet.addAll(extUserRoleMapper.getUserIdsByRoleIds(request.getRoleIds()));
            announcementReceiveTypeDTO.setRoleIds(request.getRoleIds());
        }
        if (CollectionUtils.isNotEmpty(request.getDeptIds())) {
            userSet.addAll(extDepartmentMapper.getUserIdsByDeptIds(request.getDeptIds()));
            announcementReceiveTypeDTO.setDeptIds(request.getDeptIds());
        }
        if (CollectionUtils.isNotEmpty(request.getUserIds())) {
            userSet.addAll(request.getUserIds());
            announcementReceiveTypeDTO.setUserIds(request.getUserIds());
        }
        List<String> userIds = new ArrayList<>(userSet);

        Announcement announcement = new Announcement();
        announcement.setId(IDGenerator.nextStr());
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setReceiver(JSON.toJSONString(userIds).getBytes(StandardCharsets.UTF_8));
        announcement.setReceiveType(JSON.toJSONString(announcementReceiveTypeDTO).getBytes(StandardCharsets.UTF_8));
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setCreateTime(System.currentTimeMillis());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setCreateUser(userId);
        announcement.setUpdateUser(userId);
        announcement.setNotice(false);
        //根据有效时间判断是否生成notification
        if (request.getStartTime()<= announcement.getCreateTime() && request.getEndTime() >= announcement.getCreateTime()) {
            convertNotification(userId, announcement, userIds);
            announcement.setNotice(true);
        }
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
        Set<String> userSet = new HashSet<>();
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO = new AnnouncementReceiveTypeDTO();
        if (CollectionUtils.isNotEmpty(request.getRoleIds())) {
            userSet.addAll(extUserRoleMapper.getUserIdsByRoleIds(request.getRoleIds()));
            announcementReceiveTypeDTO.setRoleIds(request.getRoleIds());
        }
        if (CollectionUtils.isNotEmpty(request.getDeptIds())) {
            userSet.addAll(extDepartmentMapper.getUserIdsByDeptIds(request.getDeptIds()));
            announcementReceiveTypeDTO.setDeptIds(request.getDeptIds());
        }
        if (CollectionUtils.isNotEmpty(request.getUserIds())) {
            userSet.addAll(request.getUserIds());
            announcementReceiveTypeDTO.setUserIds(request.getUserIds());
        }
        List<String> userIds = new ArrayList<>(userSet);
        Announcement announcement = BeanUtils.copyBean(new Announcement(), request);
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setReceiver(JSON.toJSONString(userIds).getBytes(StandardCharsets.UTF_8));
        announcement.setReceiveType(JSON.toJSONString(announcementReceiveTypeDTO).getBytes(StandardCharsets.UTF_8));
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setUpdateUser(userId);
        announcement.setNotice(true);
        announcementMapper.updateById(announcement);
        //删除旧的notice，根据接收人生成新的未读的notice
        notificationMapper.deleteByResourceId(announcement.getId());
        convertNotification(userId, announcement, userIds);
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

    /**
     * 公告转为接收人收到的通知
     * @param userId 创建人
     * @param announcement 公告
     * @param userIds 接收人集合
     */
    public void convertNotification(String userId, Announcement announcement, List<String> userIds) {
        List<Notification>notifications = new ArrayList<>();
        SubListUtils.dealForSubList(userIds, 50, (subUserIds) -> {
            for (String subUserId : subUserIds) {
                Notification notification = new Notification();
                notification.setId(IDGenerator.nextStr());
                notification.setType(NotificationConstants.Type.ANNOUNCEMENT_NOTICE.name());
                notification.setReceiver(subUserId);
                notification.setSubject(announcement.getSubject());
                notification.setStatus(NotificationConstants.Status.UNREAD.name());
                notification.setOperator(userId);
                notification.setOperation(NotificationConstants.Type.ANNOUNCEMENT_NOTICE.name());
                notification.setOrganizationId(announcement.getOrganizationId());
                notification.setResourceId(announcement.getId());
                notification.setResourceType(NotificationConstants.Type.ANNOUNCEMENT_NOTICE.name());
                notification.setResourceName(announcement.getSubject());
                notification.setContent(announcement.getContent());
                notification.setCreateUser(userId);
                notification.setUpdateUser(userId);
                notification.setCreateTime(System.currentTimeMillis());
                notification.setUpdateTime(System.currentTimeMillis());
                notifications.add(notification);
            }
            notificationBaseMapper.batchInsert(notifications);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_ANNOUNCEMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Announcement announcement = announcementMapper.selectByPrimaryKey(id);
        if (announcement == null) {
            throw new RuntimeException(Translator.get("announcement.blank"));
        }
        announcementMapper.deleteByPrimaryKey(id);
        notificationMapper.deleteByResourceId(id);
        // 添加日志上下文
        OperationLogContext.setResourceName(announcement.getSubject());
    }

    /**
     * 公告列表分页查询
     *
     * @param request request
     * @return 公告列表
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
     * @param id 公告id
     * @return AnnouncementDTO
     */
    public AnnouncementDTO detail(String id) {
        AnnouncementDTO announcementDTO = extAnnouncementMapper.selectById(id);
        if (announcementDTO == null) {
            throw new RuntimeException(Translator.get("announcement.blank"));
        }
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO  = JSON.parseObject(new String(announcementDTO.getReceiveType()), AnnouncementReceiveTypeDTO.class);
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getDeptIds())) {
            List<OptionDTO> idNameByIds = extDepartmentMapper.getIdNameByIds(announcementReceiveTypeDTO.getDeptIds());
            announcementDTO.setDeptIdName(idNameByIds);
        }
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getRoleIds())) {
            List<OptionDTO> idNameByIds = extRoleMapper.getIdNameByIds(announcementReceiveTypeDTO.getRoleIds());
            announcementDTO.setRoleIdName(idNameByIds);
        }
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getUserIds())) {
            List<OptionDTO> idNameByIds = userMapper.selectUserOptionByIds(announcementReceiveTypeDTO.getUserIds());
            announcementDTO.setUserIdName(idNameByIds);
        }
        announcementDTO.setContentText(new String(announcementDTO.getContent()));
        return announcementDTO;
    }
}

