package cn.cordys.crm.product.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 产品自定义属性大文本
 *
 * @author jianxing
 */
@Data
@Table(name = "product_field_blob")
public class ProductFieldBlob extends BaseResourceField {
}
