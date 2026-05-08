package cn.cordys.common.dto;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;

/**
 * @author jianxing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnableFieldValue extends BaseModuleFieldValue {


    @Schema(description = "是否启用")
    private Boolean enable;

    public boolean valid() {
       return super.valid() && BooleanUtils.isTrue(enable);
    }
}
