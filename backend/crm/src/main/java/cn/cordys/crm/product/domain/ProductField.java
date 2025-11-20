package cn.cordys.crm.product.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product_field")
public class ProductField extends BaseResourceField {
}
