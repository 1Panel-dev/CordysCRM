package io.cordys.crm.system.notice.message;


import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.domain.MessageTaskBlob;
import io.cordys.crm.system.mapper.ExtMessageTaskBlobMapper;
import io.cordys.crm.system.mapper.ExtMessageTaskMapper;
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
public class MessageDetailService {

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
    public List<MessageDetail> searchMessageByTypeAndOrgId(String module, String organizationId) {
        try {
            return getMessageDetails(module, organizationId);
        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<MessageDetail> getMessageDetails(String module, String organizationId) {
        List<MessageDetail> messageDetails = new ArrayList<>();
        List<MessageTask> messageTaskLists = extMessageTaskMapper.getEnableMessageTaskByReceiveTypeAndTaskType(null, module, organizationId);
        if (messageTaskLists == null) {
            return new ArrayList<>();
        }
        getMessageDetails(messageDetails, messageTaskLists);
        return messageDetails.stream()
                .sorted(Comparator.comparing(MessageDetail::getCreateTime, Comparator.nullsLast(Long::compareTo)).reversed())
                .distinct()
                .collect(Collectors.toList());
    }

    private void getMessageDetails(List<MessageDetail> messageDetails, List<MessageTask> messageTaskLists) {
        List<String> messageTaskIds = messageTaskLists.stream().map(MessageTask::getId).collect(Collectors.toList());
        List<MessageTaskBlob> messageTaskBlobs = extMessageTaskBlobMapper.getExtMessageTaskBlobByMessageTaskIds(messageTaskIds);
        Map<String, MessageTaskBlob> messageTaskBlobMap = messageTaskBlobs.stream().collect(Collectors.toMap(MessageTaskBlob::getId, item -> item));
      
        //消息通知任务以消息类型事件接收类型唯一进行分组
        Map<String, List<MessageTask>> messageTaskGroup = messageTaskLists.stream().collect(Collectors.groupingBy(t -> (t.getTaskType() + t.getEvent() + t.getReceiveType())));
        messageTaskGroup.forEach((messageTaskId, messageTaskList) -> {
            //获取同一任务所有的接收人
            MessageDetail messageDetail = new MessageDetail();
            MessageTask messageTask = messageTaskList.getFirst();
            messageDetail.setReceiverIds(JSON.parseArray(messageTask.getReceivers(), String.class));
            messageDetail.setId(messageTask.getId());
            messageDetail.setTaskType(messageTask.getTaskType());
            messageDetail.setEvent(messageTask.getEvent());
            messageDetail.setCreateTime(messageTask.getCreateTime());
            messageDetail.setOrganizationId(messageTask.getOrganizationId());
            messageDetail.setReceiveType(messageTask.getReceiveType());        
            MessageTaskBlob messageTaskBlob = messageTaskBlobMap.get(messageTask.getId());
            if (!messageTask.getUseDefaultTemplate() && messageTaskBlob.getTemplate() != null) {
                messageDetail.setTemplate(new String(messageTaskBlob.getTemplate()));
            } else {
                String template = getTemplate(messageTask.getTaskType(), messageTask.getEvent());
                messageDetail.setTemplate(template);
            }
            messageDetails.add(messageDetail);
        });
    }

    private String getTemplate(String module, String event) {
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        return defaultTemplateMap.get(module + "_" + event);
    }

}
