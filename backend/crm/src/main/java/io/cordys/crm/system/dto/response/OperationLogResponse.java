package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OperationLogResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private Long createTime;

    @Schema(description = "操作对象")
    private String module;

    @Schema(description = "操作类型")
    private String type;

    @Schema(description = "详情")
    private String details;

}