package cn.cordys.crm.system.dto.message;

import cn.cordys.common.dto.condition.FilterCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 消息通知触发类型枚举 VALUE
 */
@Data
public class TriggerTypeConfigDTO {

    @Schema(description = "动作触发配置")
    private String actionTypeConfig;

    @Schema(description = "定时提醒配置")
    private ScheduleTypeConfig scheduleTypeConfig;

    @Schema(description = "字段条件配置")
    private List<FilterCondition> fieldTypeConfig;
}
