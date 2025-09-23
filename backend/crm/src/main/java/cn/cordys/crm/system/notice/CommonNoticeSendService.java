package cn.cordys.crm.system.notice;


import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CommonNoticeSendService {
    @Resource
    private NoticeSendService noticeSendService;
    @Resource
    private BaseMapper<User> userBaseMapper;

    private static void setLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (StringUtils.containsIgnoreCase(language, "US")) {
            locale = Locale.US;
        } else if (StringUtils.containsIgnoreCase(language, "TW")) {
            locale = Locale.TAIWAN;
        }
        LocaleContextHolder.setLocale(locale);
    }

    @Async
    public void sendNotice(String module, String event, List<Map> resources, String userId, String currentOrganizationId) {
        User operator = userBaseMapper.selectByPrimaryKey(userId);
        setLanguage(operator.getLanguage());
        // 有批量操作发送多次
        for (Map resource : resources) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
            paramMap.put("Language", operator.getLanguage());
            paramMap.putAll(resource);
            paramMap.putIfAbsent("organizationId", currentOrganizationId);

            String context = getContext(event);
            List<String> relatedUsers = getRelatedUsers(resource.get("relatedUsers"));
            List<Receiver> receivers = getReceivers(relatedUsers);
            NoticeModel noticeModel = NoticeModel.builder()
                    .operator(operator.getId())
                    .context(context)
                    .paramMap(paramMap)
                    .event(event)
                    .status((String) paramMap.get("status"))
                    .excludeSelf(true)
                    .receivers(receivers)
                    .build();
            noticeSendService.send(module, noticeModel);
        }
    }

    private List<Receiver> getReceivers(List<String> relatedUsers) {
        List<Receiver> receivers = new ArrayList<>();
        for (String relatedUserId : relatedUsers) {
            receivers.add(new Receiver(relatedUserId, NotificationConstants.Type.SYSTEM_NOTICE.name()));
        }
        return receivers;
    }

    /**
     * 发送通知
     *
     * @param taskType     发送类型
     * @param event        发送事件
     * @param currentOrgId 当前组织id
     * @param resourceName 资源名称
     * @param users        需要通知的用户
     * @param excludeSelf  是否排除自己
     */
    public void sendNotice(String taskType, String event, String resourceName, String operatorId, String currentOrgId,
                           List<String> users, boolean excludeSelf) {
        sendNotice(taskType, event, Map.of("name", resourceName), operatorId, currentOrgId, users, excludeSelf);
    }

    /**
     * 发送通知
     *
     * @param taskType     发送类型
     * @param event        发送事件
     * @param currentOrgId 当前组织id
     * @param resource     消息变量的名称 以及其他变量 eg: xxxx 的名称，resource.put("name", "名称");
     * @param users        需要通知的用户
     * @param excludeSelf  是否排除自己
     */
    public void sendNotice(String taskType, String event, Map<String, Object> resource, String operatorId, String currentOrgId,
                           List<String> users, boolean excludeSelf) {
        Map<String, Object> paramMap = new HashMap<>();
        User operator = userBaseMapper.selectByPrimaryKey(operatorId);
        paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
        paramMap.put("Language", operator.getLanguage());
        paramMap.putAll(resource);
        paramMap.putIfAbsent("organizationId", currentOrgId);

        String context = getContext(event);

        List<Receiver> receivers = getReceivers(users);
        NoticeModel noticeModel = NoticeModel.builder()
                .operator(operator.getId())
                .context(context)
                .paramMap(paramMap)
                .event(event)
                .status((String) paramMap.get("status"))
                .excludeSelf(true)
                .receivers(receivers)
                .build();
        noticeSendService.sendOther(taskType, noticeModel, excludeSelf);
    }

    private List<String> getRelatedUsers(Object relatedUsers) {
        String relatedUser = (String) relatedUsers;
        List<String> relatedUserList = new ArrayList<>();
        if (StringUtils.isNotBlank(relatedUser)) {
            relatedUserList = Arrays.asList(relatedUser.split(";"));
        }
        return relatedUserList;
    }

    private String getContext(String event) {
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        return defaultTemplateMap.get(event + "_TEXT");
    }
}
