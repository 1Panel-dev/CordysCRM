package cn.cordys.crm.system.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 依据日期 配置
 */
@Data
public class ScheduleTypeConfig {

    @Schema(description = "字段id")
    private String fieldId;

    @Schema(description = "天数")
    private String day;

    @Schema(description = "时间")
    private String time;

    @Schema(description = "提醒次数枚举")
    private String remindType;
}
