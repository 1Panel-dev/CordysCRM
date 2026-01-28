package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 合同回款自定义属性大文本
 *
 * @author jianxing
 * @date 2025-02-27 14:43:46
 */
@Data
@Table(name = "contract_payment_plan_field_blob")
public class ContractPaymentPlanFieldBlob extends BaseResourceField {}
