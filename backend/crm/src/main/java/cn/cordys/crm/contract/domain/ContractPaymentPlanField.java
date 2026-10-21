package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 合同回款自定义属性
 *
 * @author jianxing
 */
@Data
@Table(name = "contract_payment_plan_field")
public class ContractPaymentPlanField extends BaseResourceSubField {
}
