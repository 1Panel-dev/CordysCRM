package io.cordys.crm.system.notice.message;


import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.domain.MessageTaskBlob;
import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.mapper.ExtMessageTaskBlobMapper;
import io.cordys.crm.system.mapper.ExtMessageTaskMapper;
import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.utils.MessageTemplateUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author guoyuqi
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MessageDetailDTOService {

   @Resource
   private ExtMessageTaskBlobMapper extMessageTaskBlobMapper;
    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;

    /**
     * 获取唯一的消息任务
     *
     * @param module  任务类型
     * @param organizationId 项目ID
     * @return List<MessageDetail>list
     */
    public List<MessageDetailDTO> searchMessageByTypeAndOrgId(String module, String organizationId) {
        try {
            return getMessageDetailDTOs(module, organizationId);
        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<MessageDetailDTO> getMessageDetailDTOs(String module, String organizationId) {
        List<MessageDetailDTO> messageDetailDTOS = new ArrayList<>();
        List<MessageTask> messageTaskLists = extMessageTaskMapper.getEnableMessageTaskByReceiveTypeAndTaskType(null, module, organizationId);
        if (messageTaskLists == null) {
            return new ArrayList<>();
        }
        getMessageDetailDTOs(messageDetailDTOS, messageTaskLists);
        return messageDetailDTOS.stream()
                .sorted(Comparator.comparing(MessageDetailDTO::getCreateTime, Comparator.nullsLast(Long::compareTo)).reversed())
                .distinct()
                .collect(Collectors.toList());
    }

    private void getMessageDetailDTOs(List<MessageDetailDTO> messageDetailDTOS, List<MessageTask> messageTaskLists) {
        List<String> messageTaskIds = messageTaskLists.stream().map(MessageTask::getId).collect(Collectors.toList());
        List<MessageTaskBlob> messageTaskBlobs = extMessageTaskBlobMapper.getExtMessageTaskBlobByMessageTaskIds(messageTaskIds);
        Map<String, MessageTaskBlob> messageTaskBlobMap = messageTaskBlobs.stream().collect(Collectors.toMap(MessageTaskBlob::getId, item -> item));
      
        //消息通知任务以消息类型事件接收类型唯一进行分组
        Map<String, List<MessageTask>> messageTaskGroup = messageTaskLists.stream().collect(Collectors.groupingBy(t -> (t.getTaskType() + t.getEvent() + t.getReceiveType())));
        messageTaskGroup.forEach((messageTaskId, messageTaskList) -> {
            //获取同一任务所有的接收人
            MessageDetailDTO messageDetailDTO = new MessageDetailDTO();
            MessageTask messageTask = messageTaskList.getFirst();
            messageDetailDTO.setReceiverIds(JSON.parseArray(messageTask.getReceivers(), String.class));
            messageDetailDTO.setId(messageTask.getId());
            messageDetailDTO.setTaskType(messageTask.getTaskType());
            messageDetailDTO.setEvent(messageTask.getEvent());
            messageDetailDTO.setCreateTime(messageTask.getCreateTime());
            messageDetailDTO.setOrganizationId(messageTask.getOrganizationId());
            messageDetailDTO.setReceiveType(messageTask.getReceiveType());
            MessageTaskBlob messageTaskBlob = messageTaskBlobMap.get(messageTask.getId());
            if (!messageTask.getUseDefaultTemplate() && messageTaskBlob.getTemplate() != null) {
                messageDetailDTO.setTemplate(new String(messageTaskBlob.getTemplate()));
            } else {
                String template = getTemplate(messageTask.getTaskType(), messageTask.getEvent());
                messageDetailDTO.setTemplate(template);
            }
            messageDetailDTOS.add(messageDetailDTO);
        });
    }

    private String getTemplate(String module, String event) {
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        return defaultTemplateMap.get(module + "_" + event);
    }

    public MessageDetailDTO buildMessageTailDTO(String module, NoticeModel noticeModel, List<String> users, String organizationId) {
        MessageDetailDTO m = new MessageDetailDTO();
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        String defaultTemplate = defaultTemplateMap.get(noticeModel.getEvent()+"_TEXT");
        m.setReceiverIds(users);
        m.setOrganizationId(organizationId);
        m.setEvent(noticeModel.getEvent());
        m.setTaskType(module);
        m.setTemplate(defaultTemplate);
        m.setReceiveType(NotificationConstants.SendType.IN_SITE);
        return m;
    }

}
