package cn.cordys.common.domain;

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
        return switch (fieldValue) {
            case null -> false;
            case String fieldValueStr when StringUtils.isBlank(fieldValueStr) -> false;
            case List fieldValueList when CollectionUtils.isEmpty(fieldValueList) -> false;
            default -> true;
        };
    }
}
