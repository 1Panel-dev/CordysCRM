package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AuthSourceRequest extends BasePageRequest {

    @Schema(description =  "认证设置id")
    private String configId;

}
