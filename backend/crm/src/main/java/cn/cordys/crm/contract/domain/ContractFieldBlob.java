package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "contract_field_blob")
public class ContractFieldBlob extends BaseResourceField {

}
