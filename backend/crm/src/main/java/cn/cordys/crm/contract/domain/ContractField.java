package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "contract_field")
public class ContractField extends BaseResourceSubField {}
