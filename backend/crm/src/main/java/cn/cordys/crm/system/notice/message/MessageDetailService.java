package cn.cordys.crm.system.notice.message;


import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MessageDetailService {

    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;

    /**
     * 获取唯一的消息任务
     *
     * @param module         任务类型
     * @param organizationId 项目ID
     *
     * @return List<MessageDetail>list
     */
    public List<MessageDetailDTO> searchMessageByTypeAndOrgId(String module, boolean useTemplate, String template, String organizationId) {
        try {
            return getMessageDetailDTOs(module, useTemplate, template, organizationId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<MessageDetailDTO> getMessageDetailDTOs(String module, boolean useTemplate, String template, String organizationId) {
        List<MessageDetailDTO> messageDetails = new ArrayList<>();
        List<MessageTask> messageTaskLists = extMessageTaskMapper.getEnableMessageTaskByReceiveTypeAndTaskType(module, organizationId);
        if (messageTaskLists == null) {
            return new ArrayList<>();
        }
        getMessageDetailDTOs(useTemplate, template, messageDetails, messageTaskLists);
        return messageDetails.stream()
                .sorted(Comparator.comparing(MessageDetailDTO::getCreateTime, Comparator.nullsLast(Long::compareTo)).reversed())
                .distinct()
                .collect(Collectors.toList());
    }

    private void getMessageDetailDTOs(boolean useTemplate, String customTemplate, List<MessageDetailDTO> messageDetails, List<MessageTask> messageTaskLists) {
        //消息通知任务以消息类型事件接收类型唯一进行分组
        Map<String, List<MessageTask>> messageTaskGroup = messageTaskLists.stream().collect(Collectors.groupingBy(t -> (t.getTaskType() + t.getEvent())));
        messageTaskGroup.forEach((messageTaskId, messageTaskList) -> {
            //获取同一任务所有的接收人
            MessageTask messageTask = messageTaskList.getFirst();
            MessageDetailDTO messageDetailDTO = new MessageDetailDTO();
            BeanUtils.copyBean(messageDetailDTO, messageTask);
            if (!useTemplate) {
                String template = MessageTemplateUtils.getTemplate(messageTask.getEvent());
                messageDetailDTO.setTemplate(template);
            } else {
                //这里特殊处理,如果使用模版，这里用调用处传来的模板
                messageDetailDTO.setTemplate(customTemplate);
            }
            messageDetails.add(messageDetailDTO);
        });
    }

}
