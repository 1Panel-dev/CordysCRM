package io.cordys.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author jianxing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseModuleFieldValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自定义属性id")
    private String fieldId;

    /**
     * 可能是数组
     */
    @Schema(description = "自定义属性值")
    private Object fieldValue;

    public boolean valid() {
        if (fieldValue == null) {
            return false;
        }
        if (fieldValue instanceof String fieldValueStr && StringUtils.isBlank(fieldValueStr)) {
            return false;
        }
        if (fieldValue instanceof List fieldValueList && CollectionUtils.isEmpty(fieldValueList)) {
            return false;
        }
        return true;
    }
}
