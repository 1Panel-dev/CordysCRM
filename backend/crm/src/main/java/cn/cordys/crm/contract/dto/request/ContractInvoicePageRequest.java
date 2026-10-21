package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class ContractInvoicePageRequest extends BasePageRequest {
    public String getContractId() {
        return null;
    }

    public String getCustomerId() {
        return null;
    }
}
