package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Data
public class ContractPaymentPlanPageRequest extends BasePageRequest {

    public String getContractId() {return null;}

    public String getCustomerId() {
        return null;
    }
}
