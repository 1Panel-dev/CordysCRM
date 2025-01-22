package io.cordys.crm.system.notice.sender;


import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.notice.*;
import io.cordys.crm.system.notice.message.MessageDetail;
import io.cordys.crm.system.notice.message.MessageTemplateUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractNoticeSender implements NoticeSender {

    @Resource
    private ExtUserMapper extUserMapper;

    protected String getContext(MessageDetail messageDetail, NoticeModel noticeModel) {
        // 处理 userIds 中包含的特殊值
        noticeModel.setReceivers(getRealUserIds(messageDetail, noticeModel, messageDetail.getEvent()));
        // 如果配置了模版就直接使用模版
        if (StringUtils.isNotBlank(messageDetail.getTemplate())) {
            return MessageTemplateUtils.getContent(messageDetail.getTemplate(), noticeModel.getParamMap());
        }
        String context = StringUtils.EMPTY;
        if (StringUtils.isBlank(context)) {
            context = noticeModel.getContext();
        }
        return MessageTemplateUtils.getContent(context, noticeModel.getParamMap());
    }

    protected String getSubjectText(MessageDetail messageDetail, NoticeModel noticeModel) {
        //目前只有公告有标题

        // 如果配置了模版就直接使用模版
        if (StringUtils.isNotBlank(messageDetail.getSubject())) {
            return MessageTemplateUtils.getContent(messageDetail.getSubject(), noticeModel.getParamMap());
        }
        String context = StringUtils.EMPTY;
        if (StringUtils.isBlank(context)) {
            context = noticeModel.getSubject();
        }
        return MessageTemplateUtils.getContent(context, noticeModel.getParamMap());
    }

    private List<Receiver> getRealUserIds(MessageDetail messageDetail, NoticeModel noticeModel, String event) {
        List<Receiver> toUsers = new ArrayList<>();
        Map<String, Object> paramMap = noticeModel.getParamMap();
        for (String userId : messageDetail.getReceiverIds()) {
            switch (userId) {
                case NotificationConstants.RelatedUser.CREATE_USER -> {
                    String createUser = (String) paramMap.get("createUser");
                    if (StringUtils.isNotBlank(createUser)) {
                        toUsers.add(new Receiver(createUser, NotificationConstants.Type.SYSTEM_NOTICE.name()));
                    } else {
                        Receiver receiver = handleCreateUser(messageDetail, noticeModel);
                        toUsers.add(Objects.requireNonNullElseGet(receiver, () -> new Receiver((String) paramMap.get(NotificationConstants.RelatedUser.OPERATOR), NotificationConstants.Type.SYSTEM_NOTICE.name())));
                    }
                }
                case NotificationConstants.RelatedUser.OPERATOR -> {
                    String operator = (String) paramMap.get(NotificationConstants.RelatedUser.OPERATOR);
                    if (StringUtils.isNotBlank(operator)) {
                        toUsers.add(new Receiver(operator, NotificationConstants.Type.SYSTEM_NOTICE.name()));
                    }
                }
                // 处理人(缺陷)
                case NotificationConstants.RelatedUser.HANDLE_USER -> {
                    String handleUser = (String) paramMap.get(NotificationConstants.RelatedUser.HANDLE_USER);
                    if (StringUtils.isNotBlank(handleUser)) {
                        toUsers.add(new Receiver(handleUser, NotificationConstants.Type.SYSTEM_NOTICE.name()));
                    }
                }
                default -> toUsers.add(new Receiver(userId, NotificationConstants.Type.SYSTEM_NOTICE.name()));
            }
        }

        // 去重复
        List<String> userIds = toUsers.stream().map(Receiver::getUserId).toList();
        LogUtils.info("userIds: ", JSON.toJSONString(userIds));
        List<User> users = getUsers(userIds, messageDetail.getOrganizationId());
        List<String> realUserIds = users.stream().map(User::getId).distinct().toList();
        return toUsers.stream().filter(t -> realUserIds.contains(t.getUserId())).distinct().toList();
    }

    protected List<User> getUsers(List<String> userIds, String organizationId) {
        if (CollectionUtils.isNotEmpty(userIds)) {
            return extUserMapper.getOrgUserByUserIds(organizationId, userIds);
        } else {
            return new ArrayList<>();
        }
    }

    private Receiver handleCreateUser(MessageDetail messageDetail, NoticeModel noticeModel) {
        String id = (String) noticeModel.getParamMap().get("id");
        if (StringUtils.isBlank(id)) {
            return null;
        }
        String taskType = messageDetail.getTaskType();

        Receiver receiver = null;
        switch (taskType) {
            default -> {
                //TODO: 处理接收人为创建人
            }
        }
        return receiver;
    }

    protected List<Receiver> getReceivers(List<Receiver> receivers, Boolean excludeSelf, String operator) {
        // 排除自己
        List<Receiver> realReceivers = new ArrayList<>();
        if (excludeSelf) {
            for (Receiver receiver : receivers) {
                if (!StringUtils.equals(receiver.getUserId(), operator)) {
                    realReceivers.add(receiver);
                }
            }
        } else {
            realReceivers = receivers;
        }
        return realReceivers;
    }
}
