package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.dto.condition.BaseCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ContractPaymentRecordStatisticRequest extends BaseCondition {

    @Schema(description = "合同id")
    private String contractId;
}
