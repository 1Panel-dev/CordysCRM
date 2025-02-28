package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product_field")
public class ProductField extends BaseResourceField {
}
