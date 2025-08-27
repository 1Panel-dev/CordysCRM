package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product_field")
public class ProductField extends BaseResourceField {
}
