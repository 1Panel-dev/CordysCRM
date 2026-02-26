package cn.cordys.crm.order.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;


@Data
public class OrderPageRequest extends BasePageRequest {
    public String getCustomerId() {
        return null;
    }
    public String getContractId() {
        return null;
    }
}
