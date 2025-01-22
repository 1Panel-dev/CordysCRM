package io.cordys.crm.system.notice;


import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.User;

import io.cordys.crm.system.notice.message.MessageTemplateUtils;
import io.cordys.mybatis.BaseMapper;
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
    private BaseMapper<User>userBaseMapper;

    @Async
    public void sendNotice(String module, String event, List<Map> resources, String userId, String currentOrganizationId) {
        User operator = userBaseMapper.selectByPrimaryKey(userId);
        setLanguage(operator.getLanguage());
        // 有批量操作发送多次
        for (Map resource : resources) {
            Map paramMap = new HashMap<>();
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
            paramMap.put("Language", operator.getLanguage());
            paramMap.putAll(resource);
            paramMap.putIfAbsent("organizationId", currentOrganizationId);

            // 占位符
            handleDefaultValues(paramMap);

            String context = getContext(event);

            List<String> relatedUsers = getRelatedUsers(resource.get("relatedUsers"));

            NoticeModel noticeModel = NoticeModel.builder()
                    .operator(operator.getId())
                    .context(context)
                    .paramMap(paramMap)
                    .event(event)
                    .status((String) paramMap.get("status"))
                    .excludeSelf(true)
                    .relatedUsers(relatedUsers)
                    .build();
            noticeSendService.send(module, noticeModel);
        }
    }

    /**
     * 发送通知
     * @param taskType
     * @param event
     * @param operator
     * @param currentProjectId
     * @param resource
     * @param extraUsers   除了消息通知配置的用户，需要额外通知的用户
     * @param excludeSelf  是否排除自己
     */
    public void sendNotice(String taskType, String event, Map resource, User operator, String currentProjectId,
                           List<String> extraUsers, boolean excludeSelf) {
        Map paramMap = new HashMap<>();
        paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
        paramMap.put("Language", operator.getLanguage());
        paramMap.putAll(resource);
        paramMap.putIfAbsent("projectId", currentProjectId);

        // 占位符
        handleDefaultValues(paramMap);

        //当前只有公告有主题
        //TODO: 判断否是公告，获取公告主题

        String context = getContext(event);

        List<String> relatedUsers = getRelatedUsers(resource.get("relatedUsers"));

        NoticeModel noticeModel = NoticeModel.builder()
                .operator(operator.getId())
                .context(context)
               // .subject(subject)
                .paramMap(paramMap)
                .event(event)
                .status((String) paramMap.get("status"))
                .excludeSelf(true)
                .relatedUsers(relatedUsers)
                .build();
        noticeSendService.sendOther(taskType, noticeModel, extraUsers, excludeSelf);
    }

    private List<String> getRelatedUsers(Object relatedUsers) {
        String relatedUser = (String) relatedUsers;
        List<String> relatedUserList = new ArrayList<>();
        if (StringUtils.isNotBlank(relatedUser)) {
            relatedUserList = Arrays.asList(relatedUser.split(";"));
        }
        return relatedUserList;
    }

    private static void setLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (StringUtils.containsIgnoreCase(language, "US")) {
            locale = Locale.US;
        } else if (StringUtils.containsIgnoreCase(language, "TW")) {
            locale = Locale.TAIWAN;
        }
        LocaleContextHolder.setLocale(locale);
    }


    /**
     * 有些默认的值，避免通知里出现 ${key}
     */
    private void handleDefaultValues(Map paramMap) {
        paramMap.put("planShareUrl", StringUtils.EMPTY); // 占位符
    }

    private String getContext(String event) {
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        return defaultTemplateMap.get(event+"_TEXT");
    }
}
