package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 合同回款记录自定义属性大文本
 * @author song-cc-rock
 */
@Data
@Table(name = "contract_payment_record_field_blob")
public class ContractPaymentRecordFieldBlob extends BaseResourceField {

}
