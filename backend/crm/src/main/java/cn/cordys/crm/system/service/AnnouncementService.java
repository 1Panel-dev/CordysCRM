package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.redis.MessagePublisher;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Announcement;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.AnnouncementReceiveTypeDTO;
import cn.cordys.crm.system.dto.convert.AnnouncementContentDTO;
import cn.cordys.crm.system.dto.log.AnnouncementLogDTO;
import cn.cordys.crm.system.dto.request.AnnouncementPageRequest;
import cn.cordys.crm.system.dto.request.AnnouncementRequest;
import cn.cordys.crm.system.dto.response.AnnouncementDTO;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.dto.response.OptionScopeDTO;
import cn.cordys.crm.system.mapper.ExtAnnouncementMapper;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.dto.NoticeRedisMessage;
import cn.cordys.crm.system.notice.sse.SseService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class AnnouncementService {

    @Resource
    private BaseMapper<Announcement> announcementMapper;
    @Resource
    private BaseMapper<Notification> notificationBaseMapper;
    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private SseService sseService;
    @Resource
    private ExtNotificationMapper notificationMapper;
    @Resource
    private ExtUserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MessagePublisher messagePublisher;


    private static final String USER_ANNOUNCE_PREFIX = "announce_user:";  // Redis 存储用户前缀
    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀
    private static final String USER_READ_PREFIX = "user_read:";  // Redis 存储用户读取前缀

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_ANNOUNCEMENT, type = LogType.ADD)
    public void add(AnnouncementRequest request, String userId) {
        Set<String> userSet = new HashSet<>();
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO = new AnnouncementReceiveTypeDTO();
        dealReceiverIds(request, userSet, announcementReceiveTypeDTO);
        List<String> userIds = new ArrayList<>(userSet);

        Announcement announcement = new Announcement();
        announcement.setId(IDGenerator.nextStr());
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setRenameUrl(request.getRenameUrl());
        announcement.setReceiver(JSON.toJSONString(userIds).getBytes(StandardCharsets.UTF_8));
        announcement.setReceiveType(JSON.toJSONString(announcementReceiveTypeDTO).getBytes(StandardCharsets.UTF_8));
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setCreateTime(System.currentTimeMillis());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setCreateUser(userId);
        announcement.setUpdateUser(userId);
        announcement.setNotice(false);
        //根据有效时间判断是否生成notification
        if (request.getStartTime() <= announcement.getCreateTime() && request.getEndTime() >= announcement.getCreateTime()) {
            convertNotification(userId, announcement, userIds);
            announcement.setNotice(true);
        }
        announcementMapper.insert(announcement);
        AnnouncementLogDTO announcementLogDTO = buildNewLogDTO(request, announcement, announcementReceiveTypeDTO);
        // 添加日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(announcement.getId())
                        .resourceName(announcement.getSubject())
                        .modifiedValue(announcementLogDTO)
                        .build()
        );
    }

    @NotNull
    private AnnouncementLogDTO buildNewLogDTO(AnnouncementRequest request, Announcement announcement, AnnouncementReceiveTypeDTO announcementReceiveTypeDTO) {
        AnnouncementLogDTO announcementLogDTO = new AnnouncementLogDTO();
        BeanUtils.copyBean(announcementLogDTO, announcement);
        List<String> receiverName = getReceiverName(announcementReceiveTypeDTO);
        announcementLogDTO.setReceiver(receiverName);
        announcementLogDTO.setContent(request.getContent());
        if (request.getStartTime() != null) {
            announcementLogDTO.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            announcementLogDTO.setEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(request.getEndTime()));
        }
        return announcementLogDTO;
    }

    @NotNull
    private AnnouncementLogDTO getOldLogDTO(Announcement originalAnnouncement) {
        AnnouncementReceiveTypeDTO oldReceiveTypeDTO = JSON.parseObject(new String(originalAnnouncement.getReceiveType()), AnnouncementReceiveTypeDTO.class);
        AnnouncementLogDTO oldLogDTO = new AnnouncementLogDTO();
        BeanUtils.copyBean(oldLogDTO, originalAnnouncement);
        List<String> receiverName = getReceiverName(oldReceiveTypeDTO);
        oldLogDTO.setReceiver(receiverName);
        oldLogDTO.setContent(new String(originalAnnouncement.getContent(), StandardCharsets.UTF_8));
        oldLogDTO.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(originalAnnouncement.getStartTime()));
        oldLogDTO.setEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(originalAnnouncement.getEndTime()));
        return oldLogDTO;
    }


    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_ANNOUNCEMENT, type = LogType.UPDATE)
    public void update(AnnouncementRequest request, String userId) {
        String organizationId = request.getOrganizationId();
        Announcement originalAnnouncement = announcementMapper.selectByPrimaryKey(request.getId());
        if (originalAnnouncement == null) {
            throw new GenericException(Translator.get("announcement.blank"));
        }
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO = new AnnouncementReceiveTypeDTO();
        Set<String> userSet = new HashSet<>();
        dealReceiverIds(request, userSet, announcementReceiveTypeDTO);
        List<String> userIds = new ArrayList<>(userSet);
        Announcement announcement = BeanUtils.copyBean(new Announcement(), request);
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setRenameUrl(request.getRenameUrl());
        announcement.setReceiver(JSON.toJSONString(userIds).getBytes(StandardCharsets.UTF_8));
        announcement.setReceiveType(JSON.toJSONString(announcementReceiveTypeDTO).getBytes(StandardCharsets.UTF_8));
        announcement.setOrganizationId(organizationId);
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setUpdateUser(userId);
        announcement.setNotice(false);
        announcementMapper.updateById(announcement);
        if (originalAnnouncement.getNotice()) {
            //删除旧的notice，根据接收人生成新的未读的notice
            String announcementId = announcement.getId();
            deleteAnnouncementKeyById(announcementId, organizationId);
            notificationMapper.deleteByResourceId(announcementId);
            //根据有效时间判断是否生成notification
            if (request.getStartTime() <= System.currentTimeMillis() && request.getEndTime() >= System.currentTimeMillis()) {
                //如果公告的有效时间在当前时间内，则生成通知
                convertNotification(userId, announcement, userIds);
            }
        }
        // 添加日志上下文
        AnnouncementLogDTO oldLogDTO = getOldLogDTO(originalAnnouncement);
        if (request.getStartTime() == null) {
            request.setStartTime(originalAnnouncement.getStartTime());
        }
        if (request.getEndTime() == null) {
            request.setEndTime(originalAnnouncement.getEndTime());
        }
        AnnouncementLogDTO announcementLogDTO = buildNewLogDTO(request, announcement, announcementReceiveTypeDTO);
        String resourceName = Optional.ofNullable(announcement.getSubject()).orElse(originalAnnouncement.getSubject());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(oldLogDTO)
                        .resourceId(originalAnnouncement.getId())
                        .resourceName(resourceName)
                        .modifiedValue(announcementLogDTO)
                        .build()
        );
    }

    private void dealReceiverIds(AnnouncementRequest request, Set<String> userSet, AnnouncementReceiveTypeDTO announcementReceiveTypeDTO) {
        if (CollectionUtils.isNotEmpty(request.getDeptIds())) {
            List<String> depIds = extDepartmentMapper.selectChildrenByIds(request.getDeptIds());
            if (CollectionUtils.isNotEmpty(depIds)) {
                userSet.addAll(extDepartmentMapper.getUserIdsByDeptIds(depIds));
            }
            announcementReceiveTypeDTO.setDeptIds(request.getDeptIds());
        }
        if (CollectionUtils.isNotEmpty(request.getUserIds())) {
            userSet.addAll(request.getUserIds());
            announcementReceiveTypeDTO.setUserIds(request.getUserIds());
        }
    }

    /**
     * 删除缓存中的公告提示
     *
     * @param announcementId 公告id
     * @param organizationId 组织Id
     */
    private void deleteAnnouncementKeyById(String announcementId, String organizationId) {
        Notification notificationRequest = new Notification();
        notificationRequest.setResourceId(announcementId);
        notificationRequest.setOrganizationId(organizationId);
        List<NotificationDTO> notificationDTOS = notificationMapper.selectByAnyOne(notificationRequest);
        for (NotificationDTO notificationDTO : notificationDTOS) {
            String notificationDTOId = notificationDTO.getId();
            stringRedisTemplate.opsForZSet().remove(USER_ANNOUNCE_PREFIX + notificationDTO.getReceiver(), notificationDTOId);
            stringRedisTemplate.delete(ANNOUNCE_PREFIX + notificationDTOId);
        }
    }

    /**
     * 公告转为接收人收到的通知
     *
     * @param userId       创建人
     * @param announcement 公告
     * @param userIds      接收人集合
     */
    public void convertNotification(String userId, Announcement announcement, List<String> userIds) {
        SubListUtils.dealForSubList(userIds, 50, (subUserIds) -> {
            List<Notification> notifications = new ArrayList<>();
            for (String subUserId : subUserIds) {
                Notification notification = new Notification();
                String id = IDGenerator.nextStr();
                notification.setId(id);
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
                AnnouncementContentDTO announcementContentDTO = new AnnouncementContentDTO();
                announcementContentDTO.setUrl(announcement.getUrl());
                announcementContentDTO.setRenameUrl(announcement.getRenameUrl());
                announcementContentDTO.setContent(new String(announcement.getContent()));
                notification.setContent(JSON.toJSONString(announcementContentDTO).getBytes());
                notification.setCreateUser(userId);
                notification.setUpdateUser(userId);
                notification.setCreateTime(System.currentTimeMillis());
                notification.setUpdateTime(System.currentTimeMillis());
                notifications.add(notification);
                //根据公告的有效时间判断是否需要发送通知
                if (announcement.getStartTime() <= System.currentTimeMillis() && announcement.getEndTime() >= System.currentTimeMillis()) {
                    NotificationDTO notificationDTO = new NotificationDTO();
                    BeanUtils.copyBean(notificationDTO, notification);
                    notificationDTO.setContentText(JSON.toJSONString(announcementContentDTO));
                    String messageText = JSON.toJSONString(notificationDTO);

                    stringRedisTemplate.opsForZSet().add(USER_ANNOUNCE_PREFIX + subUserId, id, System.currentTimeMillis());
                    stringRedisTemplate.opsForValue().set(ANNOUNCE_PREFIX + id, messageText);
                    //更新用户的已读全部消息状态 0 为未读，1为已读
                    stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + subUserId, "False");
                    // 发送消息
                    NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
                    noticeRedisMessage.setMessage(subUserId);
                    noticeRedisMessage.setNoticeType(NotificationConstants.Type.ANNOUNCEMENT_NOTICE.toString());
                    messagePublisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
                    //sseService.broadcastPeriodically(subUserId, NotificationConstants.Type.ANNOUNCEMENT_NOTICE.toString());
                }
            }
            notificationBaseMapper.batchInsert(notifications);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_ANNOUNCEMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Announcement announcement = announcementMapper.selectByPrimaryKey(id);
        if (announcement == null) {
            throw new RuntimeException(Translator.get("announcement.blank"));
        }
        deleteAnnouncementKeyById(id, announcement.getOrganizationId());
        announcementMapper.deleteByPrimaryKey(id);
        notificationMapper.deleteByResourceId(id);

        // 添加日志上下文
        OperationLogContext.setResourceName(announcement.getSubject());
    }

    /**
     * 公告列表分页查询
     *
     * @param request request
     *
     * @return 公告列表
     */
    public List<AnnouncementDTO> page(AnnouncementPageRequest request) {
        List<AnnouncementDTO> announcementDTOS = extAnnouncementMapper.selectByBaseRequest(request);
        if (CollectionUtils.isNotEmpty(announcementDTOS)) {
            for (AnnouncementDTO announcementDTO : announcementDTOS) {
                announcementDTO.setContentText(new String(announcementDTO.getContent(), StandardCharsets.UTF_8));
                setReceiverNameOption(announcementDTO);
            }
        }
        return announcementDTOS;
    }

    /**
     * 获取公告
     *
     * @param id 公告id
     *
     * @return AnnouncementDTO
     */
    public AnnouncementDTO detail(String id) {
        AnnouncementDTO announcementDTO = extAnnouncementMapper.selectById(id);
        if (announcementDTO == null) {
            throw new RuntimeException(Translator.get("announcement.blank"));
        }
        setReceiverNameOption(announcementDTO);
        announcementDTO.setContentText(new String(announcementDTO.getContent(), StandardCharsets.UTF_8));
        return announcementDTO;
    }

    private void setReceiverNameOption(AnnouncementDTO announcementDTO) {
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO = JSON.parseObject(new String(announcementDTO.getReceiveType()), AnnouncementReceiveTypeDTO.class);
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getDeptIds())) {
            List<OptionDTO> idNameByIds = extDepartmentMapper.getIdNameByIds(announcementReceiveTypeDTO.getDeptIds());
            List<OptionScopeDTO> optionScopeDTOList = new ArrayList<>();
            for (OptionDTO idNameById : idNameByIds) {
                OptionScopeDTO optionScopeDTO = new OptionScopeDTO();
                BeanUtils.copyBean(optionScopeDTO, idNameById);
                optionScopeDTO.setScope("DEPARTMENT");
                optionScopeDTOList.add(optionScopeDTO);
            }
            announcementDTO.setDeptIdName(optionScopeDTOList);
        }
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getUserIds())) {
            List<OptionDTO> idNameByIds = userMapper.selectUserOptionByIds(announcementReceiveTypeDTO.getUserIds());
            List<OptionScopeDTO> optionScopeDTOList = new ArrayList<>();
            for (OptionDTO idNameById : idNameByIds) {
                OptionScopeDTO optionScopeDTO = new OptionScopeDTO();
                BeanUtils.copyBean(optionScopeDTO, idNameById);
                optionScopeDTO.setScope("USER");
                optionScopeDTOList.add(optionScopeDTO);
            }
            announcementDTO.setUserIdName(optionScopeDTOList);
        }
    }

    public List<String> getReceiverName(AnnouncementReceiveTypeDTO announcementReceiveTypeDTO) {
        List<String> returnNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getDeptIds())) {
            List<String> names = extDepartmentMapper.getNameByIds(announcementReceiveTypeDTO.getDeptIds());
            returnNames.addAll(names);
        }
        if (CollectionUtils.isNotEmpty(announcementReceiveTypeDTO.getUserIds())) {
            List<String> names = userMapper.selectUserNameByIds(announcementReceiveTypeDTO.getUserIds());
            returnNames.addAll(names);
        }
        return returnNames;

    }

}

