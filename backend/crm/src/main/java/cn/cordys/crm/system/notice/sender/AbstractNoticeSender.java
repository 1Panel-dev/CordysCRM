package cn.cordys.crm.system.notice.sender;


import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractNoticeSender implements NoticeSender {

    @Resource
    private ExtUserMapper extUserMapper;

    protected String getContext(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        // 处理 userIds 中包含的特殊值
        noticeModel.setReceivers(getRealUserIds(noticeModel, messageDetailDTO.getOrganizationId()));
        // 如果配置了模版就直接使用模版
        if (StringUtils.isNotBlank(messageDetailDTO.getTemplate())) {
            return MessageTemplateUtils.getContent(messageDetailDTO.getTemplate(), noticeModel.getParamMap());
        }
        String context = StringUtils.EMPTY;
        if (StringUtils.isBlank(context)) {
            context = noticeModel.getContext();
        }
        return MessageTemplateUtils.getContent(context, noticeModel.getParamMap());
    }

    protected String getSubjectText(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        //目前只有公告有标题消息通知使用事件+“通知”
        // 如果配置了模版就直接使用模版
        if (StringUtils.isNotBlank(messageDetailDTO.getSubject())) {
            return MessageTemplateUtils.getContent(messageDetailDTO.getSubject(), noticeModel.getParamMap());
        }
        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();
        String eventText = eventMap.get(messageDetailDTO.getEvent());
        String context = eventText + Translator.get("notice.event.subject");
        if (StringUtils.isBlank(context)) {
            context = noticeModel.getSubject();
        }
        return MessageTemplateUtils.getContent(context, noticeModel.getParamMap());
    }

    private List<Receiver> getRealUserIds(NoticeModel noticeModel, String orgId) {
        List<Receiver> toUsers = noticeModel.getReceivers();
        if (CollectionUtils.isEmpty(toUsers)) {
            toUsers = new ArrayList<>();
        }

        // 去重复
        List<String> userIds = toUsers.stream().map(Receiver::getUserId).toList();
        List<User> users = getUsers(userIds, orgId);
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

    protected List<String> getResourceUserIds(List<String> userIds, String organizationId) {
        if (CollectionUtils.isNotEmpty(userIds)) {
            return extUserMapper.getOrgUserResourceIds(userIds, organizationId);
        } else {
            return new ArrayList<>();
        }
    }

    protected List<Receiver> getReceivers(List<Receiver> receivers, Boolean excludeSelf, String operator) {
        // 排除自己
        List<Receiver> realReceivers = new ArrayList<>();
        if (excludeSelf) {
            for (Receiver receiver : receivers) {
                if (!Strings.CS.equals(receiver.getUserId(), operator)) {
                    realReceivers.add(receiver);
                }
            }
        } else {
            realReceivers = receivers;
        }
        return realReceivers;
    }
}
