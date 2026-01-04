package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "contract_invoice_field")
public class ContractInvoiceField extends BaseResourceSubField {

}
