package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author guoyuqi
 */
@Data
public class ProductPageRequest extends BasePageRequest {

    @Schema(description = "状态：1-上架，2-下架")
    private String status;

}
