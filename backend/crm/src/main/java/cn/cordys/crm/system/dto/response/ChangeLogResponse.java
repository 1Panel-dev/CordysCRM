package cn.cordys.crm.system.dto.response;

import cn.cordys.common.dto.JsonDifferenceDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ChangeLogResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志id")
    private String id;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作时间")
    private Long createTime;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "操作类型")
    private String type;

    @Schema(description = "资源名称")
    private String resourceName;

    @Schema(description = "日志描述")
    private String detail;

    @Schema(description = "字段变更明细")
    private List<JsonDifferenceDTO> diffs = List.of();
}
