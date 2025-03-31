package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class JsonDifferenceDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段")
    private String column;

    @Schema(description = "原值")
    private Object oldValue;

    @Schema(description = "新值")
    private Object newValue;


    @Schema(description = "字段名称")
    private String columnName;

    @Schema(description = "原值结果")
    private Object oldValueName;

    @Schema(description = "新值结果")
    private Object newValueName;

    @Schema(description = "类型", examples = {"add/新增", "removed/删除", "modified/修改"})
    private String type;

}
