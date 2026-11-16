package cn.cordys.crm.customer.dto.request;

import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import jakarta.validation.constraints.NotBlank;

/**
 * 客户详情回款记录统计请求。
 */
public class CustomerContractPaymentRecordStatisticRequest extends ContractPaymentRecordPageRequest {

    @Override
    @NotBlank
    public String getCustomerId() {
        return super.getCustomerId();
    }
}
