package io.cordys.crm.system.notice;


import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.User;

import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.notice.common.Receiver;
import io.cordys.crm.system.utils.MessageTemplateUtils;
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

            String context = getContext(event);


            NoticeModel noticeModel = NoticeModel.builder()
                    .operator(operator.getId())
                    .context(context)
                    .paramMap(paramMap)
                    .event(event)
                    .status((String) paramMap.get("status"))
                    .excludeSelf(true)
                    .build();
            noticeSendService.send(module, noticeModel);
        }
    }

    /**
     * 发送通知
     * @param taskType 发送类型
     * @param event 发送事件
     * @param currentOrgId  当前组织id
     * @param resource 消息变量的名称 eg: xxxx 的名称，resource.put("name", "名称");
     * @param users   需要通知的用户
     * @param excludeSelf  是否排除自己
     */
    public void sendNotice(String taskType, String event, Map resource, String operatorId, String currentOrgId,
                           List<String> users, boolean excludeSelf) {
        Map paramMap = new HashMap<>();
        User operator = userBaseMapper.selectByPrimaryKey(operatorId);
        paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
        paramMap.put("Language", operator.getLanguage());
        paramMap.putAll(resource);
        paramMap.putIfAbsent("organizationId", currentOrgId);

        String context = getContext(event);

        List<Receiver> receivers = new ArrayList<>();
        for (String userId :users) {
            receivers.add(new Receiver(userId, NotificationConstants.Type.SYSTEM_NOTICE.name()));
        }

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


    private static void setLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (StringUtils.containsIgnoreCase(language, "US")) {
            locale = Locale.US;
        } else if (StringUtils.containsIgnoreCase(language, "TW")) {
            locale = Locale.TAIWAN;
        }
        LocaleContextHolder.setLocale(locale);
    }

    private String getContext(String event) {
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        return defaultTemplateMap.get(event+"_TEXT");
    }
}
