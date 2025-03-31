package io.cordys.crm.system.notice.message;


import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.mapper.ExtMessageTaskMapper;
import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.utils.MessageTemplateUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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
        List<MessageTask> messageTaskLists = extMessageTaskMapper.getEnableMessageTaskByReceiveTypeAndTaskType(module, organizationId);
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
        //消息通知任务以消息类型事件接收类型唯一进行分组
        Map<String, List<MessageTask>> messageTaskGroup = messageTaskLists.stream().collect(Collectors.groupingBy(t -> (t.getTaskType() + t.getEvent())));
        messageTaskGroup.forEach((messageTaskId, messageTaskList) -> {
            //获取同一任务所有的接收人
            MessageTask messageTask = messageTaskList.getFirst();
            MessageDetailDTO messageDetailDTO = new MessageDetailDTO();
            BeanUtils.copyBean(messageDetailDTO,messageTask);
            if (messageTask.getTemplate()==null) {
                String template = MessageTemplateUtils.getTemplate(messageTask.getEvent());
                messageDetailDTO.setTemplate(template);
            } else {
                messageDetailDTO.setTemplate(new String(messageTask.getTemplate()));

            }
            messageDetailDTOS.add(messageDetailDTO);
        });
    }



    public MessageDetailDTO buildMessageTailDTO(String module, NoticeModel noticeModel, String organizationId) {
        MessageDetailDTO m = new MessageDetailDTO();
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        String defaultTemplate = defaultTemplateMap.get(noticeModel.getEvent()+"_TEXT");
        m.setOrganizationId(organizationId);
        m.setEvent(noticeModel.getEvent());
        m.setTaskType(module);
        m.setTemplate(defaultTemplate);
        m.setSysEnable(true);
        m.setEmailEnable(false);
        return m;
    }

}
