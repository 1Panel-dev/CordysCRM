package cn.cordys.crm.order.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class OrderPageRequest extends BasePageRequest {

    @Schema(description = "看板模式")
    private Boolean board = false;
    public String getCustomerId() {
        return null;
    }
    public String getContractId() {
        return null;
    }
}
