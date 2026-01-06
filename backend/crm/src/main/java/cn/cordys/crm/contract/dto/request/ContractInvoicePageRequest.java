package cn.cordys.crm.contract.dto.request;

import lombok.Data;
import cn.cordys.common.dto.BasePageRequest;

/**
 *
 * @author jianxing
 * @date 2025-12-29 18:22:59
 */
@Data
public class ContractInvoicePageRequest extends BasePageRequest {
    public String getContractId() {return null;}
    public String getCustomerId() {return null;}
}
