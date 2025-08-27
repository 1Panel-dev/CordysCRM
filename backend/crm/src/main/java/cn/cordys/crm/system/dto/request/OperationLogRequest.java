package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OperationLogRequest extends BasePageRequest {

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "开始时间")
    @NotNull(message = "{start_time_is_null}")
    private Long startTime;

    @Schema(description = "结束时间")
    @NotNull(message = "{end_time_is_null}")
    private Long endTime;

    @Schema(description = "操作类型")
    private String type;

    @Schema(description = "操作对象")
    private String module;
}