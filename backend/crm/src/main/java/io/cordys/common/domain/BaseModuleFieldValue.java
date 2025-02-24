package io.cordys.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseModuleFieldValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自定义属性id")
    private String fieldId;

    @Schema(description = "自定义属性值")
    private String fieldValue;
}
