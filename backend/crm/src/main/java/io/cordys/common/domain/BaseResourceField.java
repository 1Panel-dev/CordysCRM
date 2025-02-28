package io.cordys.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianxing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResourceField extends BaseModuleFieldValue {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "资源id")
    private String resourceId;
}
