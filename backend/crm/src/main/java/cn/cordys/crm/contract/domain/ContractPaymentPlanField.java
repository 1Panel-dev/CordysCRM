package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 合同回款自定义属性
 *
 * @author jianxing
 * @date 2025-02-10 18:12:46
 */
@Data
@Table(name = "contract_payment_plan_field")
public class ContractPaymentPlanField extends BaseResourceField {
}
