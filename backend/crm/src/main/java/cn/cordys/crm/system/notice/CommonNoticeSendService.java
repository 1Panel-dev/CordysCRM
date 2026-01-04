package cn.cordys.crm.system.notice;


import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.DepartmentCommander;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
    @Resource
    private BaseMapper<DepartmentCommander> departmentCommanderMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;

    private static void setLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (Strings.CI.contains(language, "US")) {
            locale = Locale.US;
        } else if (Strings.CI.contains(language, "TW")) {
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
                .excludeSelf(excludeSelf)
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

    /**
     * 获取消息接收用户ID列表
     *
     * @param messageTaskConfigDTO 消息任务配置DTO
     * @param createUser           创建人ID
     * @param owner                责任人ID
     * @param orgId                组织ID
     * @return 消息接收用户ID列表
     */
    public List<String> getNoticeReceiveUserIds(MessageTaskConfigDTO messageTaskConfigDTO, String createUser, String owner, String orgId) {
        List<String> receiveUserIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(messageTaskConfigDTO.getUserIds())) {
            if (messageTaskConfigDTO.getUserIds().contains(NotificationConstants.RelatedUser.CREATE_USER)) {
                receiveUserIds.add(createUser);
            }
            if (messageTaskConfigDTO.getUserIds().contains(NotificationConstants.RelatedUser.OWNER)) {
                receiveUserIds.add(owner);
            }
            List<String> normalUserIds = messageTaskConfigDTO.getUserIds().stream()
                    .filter(userId -> !userId.equals(NotificationConstants.RelatedUser.CREATE_USER)
                            && !userId.equals(NotificationConstants.RelatedUser.OWNER))
                    .toList();
            receiveUserIds.addAll(normalUserIds);
        }
        if (messageTaskConfigDTO.isRoleEnable() && CollectionUtils.isNotEmpty(messageTaskConfigDTO.getRoleIds())) {
            receiveUserIds.addAll(extUserMapper.selectUserIdsByRoleIds(messageTaskConfigDTO.getRoleIds()));
        }

        if (!messageTaskConfigDTO.isOwnerEnable()) {
            return receiveUserIds;
        }

        String departmentId = extOrganizationUserMapper.getDepartmentByUserId(owner);

        if (StringUtils.isBlank(departmentId)) {
            return receiveUserIds;
        }

        List<String> departmentIds = new ArrayList<>();
        departmentIds.add(departmentId);
        int ownerLevel = messageTaskConfigDTO.getOwnerLevel();

        if (ownerLevel > 0) {
            String currentDepartmentId = departmentId;
            for (int i = 1; i < ownerLevel; i++) {
                String parentId = extDepartmentMapper.getParentIdById(currentDepartmentId);
                if (StringUtils.isBlank(parentId)) {
                    break;
                }
                departmentIds.add(parentId);
                currentDepartmentId = parentId;
            }
        }
        if (CollectionUtils.isEmpty(departmentIds)) {
            return receiveUserIds;
        }
        List<DepartmentCommander> departmentCommanderList = departmentCommanderMapper.selectListByLambda(new LambdaQueryWrapper<DepartmentCommander>()
                .in(DepartmentCommander::getDepartmentId, departmentIds));
        if (CollectionUtils.isEmpty(departmentCommanderList)) {
            return receiveUserIds;
        }
        List<String> deptCommanderUserIds = departmentCommanderList.stream()
                .map(DepartmentCommander::getUserId)
                .toList();
        receiveUserIds.addAll(deptCommanderUserIds);

        return receiveUserIds.stream().distinct().toList();

    }

}
