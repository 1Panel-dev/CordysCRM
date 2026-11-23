package cn.cordys.crm.system.dto.message;

import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import lombok.Data;

@Data
public class MessageLogDTO {

    private MessageTask task;

    private MessageTaskConfig taskConfig;
}
