package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 合同回款自定义属性大文本
 *
 * @author jianxing
 */
@Data
@Table(name = "contract_payment_plan_field_blob")
public class ContractPaymentPlanFieldBlob extends BaseResourceSubField {
}
